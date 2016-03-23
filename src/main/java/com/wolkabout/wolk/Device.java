package com.wolkabout.wolk;

/**
 * Device for which the publishing is done.
 */
public class Device {

    /**
     * Serial number obtained for the device registration.
     */
    protected String serialId;

    /**
     * Password obtained after the device has been registered.
     */
    protected String password;

    public String getSerialId() {
        return serialId;
    }

    public void setSerialId(final String serialId) {
        this.serialId = serialId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

}
