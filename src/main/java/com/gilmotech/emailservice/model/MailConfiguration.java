package com.gilmotech.emailservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailConfiguration {

    private AppCode appCode;
    private MailType mailType;
    private String fromAddress;
    private String fromName;
    private List<String> toAddresses = new ArrayList<>();
    private List<String> ccAddresses = new ArrayList<>();
    private List<String> bccAddresses = new ArrayList<>();
    private String replyTo;
    private String subject;
    private String templatePath;
    private String templatePathConfirmation;
    private boolean active = true;
}