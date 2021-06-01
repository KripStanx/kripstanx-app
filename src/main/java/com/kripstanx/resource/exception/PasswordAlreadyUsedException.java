package com.kripstanx.resource.exception;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

public class PasswordAlreadyUsedException extends AbstractThrowableProblem {

    private static final long serialVersionUID = 1L;

    public PasswordAlreadyUsedException(String username, int passwordCheckLimit) {
        super(ErrorConstants.INVALID_PASSWORD_TYPE,
              String.format("Password for user '%s' cannot be among the last %d used ones!", username, passwordCheckLimit),
              Status.BAD_REQUEST);
    }
}
