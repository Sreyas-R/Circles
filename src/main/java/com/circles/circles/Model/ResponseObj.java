package com.circles.circles.Model;

public class ResponseObj {
    String errorMessage;
    String succMessage;
    String inviteLink;
    Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSuccMessage() {
        return succMessage;
    }

    public void setSuccMessage(String succMessage) {
        this.succMessage = succMessage;
    }

    public String getInviteLink() {
        return inviteLink;
    }

    public void setInviteLink(String inviteLink) {
        this.inviteLink = inviteLink;
    }
}
