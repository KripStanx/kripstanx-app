package com.kripstanx.service;

import com.kripstanx.constants.Constants;
import com.kripstanx.domain.Authority;
import com.kripstanx.domain.PasswordHistoryEntry;
import com.kripstanx.domain.User;
import com.kripstanx.repository.AuthorityRepository;
import com.kripstanx.repository.PasswordHistoryEntryRepository;
import com.kripstanx.repository.UserRepository;
import com.kripstanx.resource.exception.InvalidPasswordException;
import com.kripstanx.resource.exception.PasswordAlreadyUsedException;
import com.kripstanx.security.AuthoritiesConstants;
import com.kripstanx.security.SecurityUtils;
import com.kripstanx.service.builder.QuickSearchSpecBuilder;
import com.kripstanx.service.dto.UserDTO;
import com.kripstanx.service.mapper.UserMapper;
import com.kripstanx.service.util.RandomUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

    public static final int COUNT_OF_LAST_PASSWORDS_FOR_VALIDATING = 6;
    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthorityRepository authorityRepository;

//    private final CacheManager cacheManager;

    private final PasswordHistoryEntryRepository passwordHistoryEntryRepository;

    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthorityRepository authorityRepository,
                       /*CacheManager cacheManager,*/
                       PasswordHistoryEntryRepository passwordHistoryEntryRepository,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
//        this.cacheManager = cacheManager;
        this.passwordHistoryEntryRepository = passwordHistoryEntryRepository;
        this.userMapper = userMapper;
    }

    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository.findOneByActivationKey(key)
                             .map(user -> {
                                 // activate given user for the registration key.
                                 user.setActiveIndicator(true);
                                 user.setActivationKey(null);
                                 this.clearUserCaches(user);
                                 log.debug("Activated user: {}", user);
                                 return user;
                             });
    }


    public Optional<User> getUserForResetKey(String key) {
        return userRepository.findOneByResetKey(key)
                             .filter(user -> user.getResetDate()
                                                 .isAfter(Instant.now().minusSeconds(Duration.ofDays(1).getSeconds())));

    }

    public Optional<User> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);

        return getUserForResetKey(key)
            .map(user -> {
                String encryptedPassword = passwordEncoder.encode(newPassword);
                user.setPassword(encryptedPassword);
                user.setPasswordExpirationDate(Instant.now().plus(Duration.ofDays(90)));
                user.setResetKey(null);
                user.setResetDate(null);

                validateIfPasswordAlreadyUsed(user.getUsername(),
                                              newPassword,
                                              COUNT_OF_LAST_PASSWORDS_FOR_VALIDATING);
                passwordHistoryEntryRepository.save(new PasswordHistoryEntry().username(user.getUsername())
                                                                              .password(
                                                                                  encryptedPassword)
                                                                              .entryDate(Instant.now()));

                this.clearUserCaches(user);
                return user;
            });
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository.findOneByEmailIgnoreCase(mail)
                             .filter(User::isActiveIndicator)
                             .map(user -> {
                                 user.setResetKey(RandomUtil.generateResetKey());
                                 user.setResetDate(Instant.now());
                                 this.clearUserCaches(user);
                                 return user;
                             });
    }

    public User registerUser(UserDTO userDTO, String password) {
        User newUser = new User();
        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setUsername(userDTO.getUsername());
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userDTO.getFirstName());
        newUser.setLastName(userDTO.getLastName());
        newUser.setEmail(userDTO.getEmail());
        newUser.setImageUrl(userDTO.getImageUrl());
        newUser.setLangKey(userDTO.getLangKey());
        newUser.setActiveIndicator(true);
        newUser.setResetKey(RandomUtil.generateResetKey());
        newUser.setResetDate(Instant.now());
        newUser.setPasswordExpirationDate(Instant.now().plus(Duration.ofDays(90)));
        // new user gets registration key
        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);

        validateIfPasswordAlreadyUsed(newUser.getUsername(), password,
                                      COUNT_OF_LAST_PASSWORDS_FOR_VALIDATING);
        passwordHistoryEntryRepository.save(new PasswordHistoryEntry().username(newUser.getUsername())
                                                                      .password(encryptedPassword)
                                                                      .entryDate(Instant.now()));

        this.clearUserCaches(newUser);

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setImageUrl(userDTO.getImageUrl());
        if (userDTO.getLangKey() == null) {
            user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
        } else {
            user.setLangKey(userDTO.getLangKey());
        }
        if (userDTO.getAuthorities() != null) {
            Set<Authority> authorities = userDTO.getAuthorities().stream()
                                                .filter(Objects::nonNull)
                                                .map(authorityRepository::findById)
                                                .filter(Optional::isPresent)
                                                .map(Optional::get)
                                                .collect(Collectors.toSet());
            user.setAuthorities(authorities);
        }
        addDefaultAuthorities(user);

        String generatedPassword = RandomUtil.generatePassword();
        String encryptedPassword = passwordEncoder.encode(generatedPassword);
        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        user.setPasswordExpirationDate(Instant.now().plus(Duration.ofDays(90)));
        user.setActiveIndicator(true);
        userRepository.save(user);

        validateIfPasswordAlreadyUsed(user.getUsername(), generatedPassword,
                                      COUNT_OF_LAST_PASSWORDS_FOR_VALIDATING);
        passwordHistoryEntryRepository.save(new PasswordHistoryEntry().username(user.getUsername())
                                                                      .password(encryptedPassword)
                                                                      .entryDate(Instant.now()));

        this.clearUserCaches(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param firstName name of user
     * @param lastName name of user
     * @param email email id of user
     * @param langKey language key
     * @param imageUrl image URL of user
     */
    public void updateUser(String firstName, String lastName, String email, String langKey, String imageUrl) {
        SecurityUtils.getCurrentUserLogin()
                     .flatMap(userRepository::findOneByUsername)
                     .ifPresent(user -> {
                         user.setFirstName(firstName);
                         user.setLastName(lastName);
                         user.setEmail(email);
                         user.setLangKey(langKey);
                         user.setImageUrl(imageUrl);
                         this.clearUserCaches(user);
                         log.debug("Changed Information for User: {}", user);
                     });
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update
     * @return updated user
     */
    public Optional<UserDTO> updateUser(UserDTO userDTO) {
        return Optional.of(userRepository
                               .findById(userDTO.getId()))
                       .filter(Optional::isPresent)
                       .map(Optional::get)
                       .map(user -> {
                           this.clearUserCaches(user);
                           user.setUsername(userDTO.getUsername());
                           user.setFirstName(userDTO.getFirstName());
                           user.setLastName(userDTO.getLastName());
                           user.setEmail(userDTO.getEmail());
                           user.setImageUrl(userDTO.getImageUrl());
                           user.setActiveIndicator(userDTO.isActiveIndicator());
                           user.setLangKey(userDTO.getLangKey());
                           Set<Authority> managedAuthorities = user.getAuthorities();
                           managedAuthorities.clear();
                           userDTO.getAuthorities().stream()
                                  .filter(Objects::nonNull)
                                  .map(authorityRepository::findById)
                                  .filter(Optional::isPresent)
                                  .map(Optional::get)
                                  .forEach(managedAuthorities::add);
                           addDefaultAuthorities(user);
                           this.clearUserCaches(user);
                           log.debug("Changed Information for User: {}", user);
                           return user;
                       })
                       .map(userMapper::toDto);
    }

    private User addDefaultAuthorities(User user) {
        if (user.getAuthorities() == null) {
            user.setAuthorities(new HashSet<>());
        }
        //The USER authorities cannot be deletable
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(user.getAuthorities()::add);
        return user;
    }

    /**
     * Lock (turn off activated) an active user
     *
     * @param username loginID of user
     */
    public void lockActiveUser(String username) {
        userRepository.findOneByUsername(username.toLowerCase(Locale.ENGLISH))
                      .ifPresent(user -> {
                          user.setActiveIndicator(false);
                          this.clearUserCaches(user);
                          log.debug("Locked User: {}", user);
                      });
    }

    public void deleteUser(String username) {
        userRepository.findOneByUsername(username).ifPresent(user -> {
            userMapper.toDto(user);
            userRepository.delete(user);
            passwordHistoryEntryRepository.findByUsername(user.getUsername())
                                          .forEach(passwordHistoryEntryRepository::delete);
            this.clearUserCaches(user);
            log.debug("Deleted User: {}", user);
        });
    }

    public void changePassword(String currentClearTextPassword, String newPassword) {
        SecurityUtils.getCurrentUserLogin()
                     .flatMap(userRepository::findOneByUsername)
                     .ifPresent(user -> {
                         String currentEncryptedPassword = user.getPassword();
                         if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                             throw new InvalidPasswordException();
                         }
                         String encryptedPassword = passwordEncoder.encode(newPassword);
                         user.setPassword(encryptedPassword);
                         user.setPasswordExpirationDate(Instant.now().plus(Duration.ofDays(90)));

                         validateIfPasswordAlreadyUsed(user.getUsername(),
                                                       newPassword,
                                                       COUNT_OF_LAST_PASSWORDS_FOR_VALIDATING);
                         passwordHistoryEntryRepository.save(new PasswordHistoryEntry().username(user.getUsername())
                                                                                       .password(encryptedPassword)
                                                                                       .entryDate(Instant.now()));

                         this.clearUserCaches(user);

                         log.debug("Changed password for User: {}", user);
                     });
    }

    public Boolean isUserPasswordExpired(String login) {
        Optional<User> mayBeUser = userRepository.findOneByUsername(login);
        return mayBeUser.map(user -> user.getPasswordExpirationDate().isBefore(Instant.now())).orElse(false);
    }

    public Optional<User> findOneByLogin(String login) {
        return userRepository.findOneByUsername(login);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllManagedUsers(Pageable pageable, String quickSearchText) {
        if (StringUtils.isNotEmpty(quickSearchText)) {
            Specification<User> specification = new QuickSearchSpecBuilder<User>(quickSearchText)
//                .addStringAttribute(User_.login)
//                .addStringAttribute(User_.email)
//                .addStringAttribute(User_.name)
                .build();
            return userRepository.findAll(specification, pageable).map((userMapper::toDto));
        } else {
            return userRepository.findAll(pageable).map(userMapper::toDto);
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByUsername(login);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities(Long id) {
        return userRepository.findOneWithAuthoritiesById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByUsername);
    }

    /**
     * @return a list of all the authorities
     */
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).collect(Collectors.toList());
    }

    public void clearUserCaches(User user) {
//        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evict(user.getLogin());
//        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evict(user.getEmail());
    }

    public void clearUserCachesForTests() {
//        cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).clear();
//        cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE).clear();
    }

    public void validateIfPasswordAlreadyUsed(String username, String clearTextPassword, int passwordCheckLimit) {
        List<PasswordHistoryEntry> passwordHistoryEntries = passwordHistoryEntryRepository
            .findByUsernameOrderByEntryDateDesc(username);

        boolean found = passwordHistoryEntries.stream()
                                              .limit(passwordCheckLimit)
                                              .map(PasswordHistoryEntry::getPassword)
                                              .anyMatch(encodedPassword -> matchPassword(clearTextPassword,
                                                                                         encodedPassword));
        if (found) {
            throw new PasswordAlreadyUsedException(username, passwordCheckLimit);
        }
    }

    private boolean matchPassword(CharSequence clearTextPassword, String encodedPasswordInHistory) {
        return passwordEncoder.matches(clearTextPassword, encodedPasswordInHistory);
    }
}
