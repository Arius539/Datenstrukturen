package org.fpj.messaging.domain;

import org.fpj.users.domain.User;

public record DirectMessageRow(User sender, User recipient, String content) {

}
