package com.diemlife.models;

public class UserEmailDestinationDTO implements UserEmailDestination {

    private final Integer id;
    private final String userName;
    private final String firstName;
    private final String lastName;
    private final String email;

    public UserEmailDestinationDTO(final Integer id,
                                   final String userName,
                                   final String firstName,
                                   final String lastName,
                                   final String email) {
        this.id = id;
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getEmail() {
        return email;
    }

}
