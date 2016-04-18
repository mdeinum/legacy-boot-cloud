package com.apress.prospringmvc.bookstore.domain.support;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import com.apress.prospringmvc.bookstore.domain.Account;
import com.apress.prospringmvc.bookstore.domain.Address;
import com.apress.prospringmvc.bookstore.domain.Permission;
import com.apress.prospringmvc.bookstore.domain.Role;

/**
 * Builds {@link Account} domain objects
 * 
 * @author Marten Deinum
 * @author Koen Serneels
 * 
 */
@Component
public class AccountBuilder extends EntityBuilder<Account> {

    @Override
    void initProduct() {
        this.entity = new Account();
    }

    public AccountBuilder credentials(String username, String password) {
        this.entity.setUsername(username);
        this.entity.setPassword(DigestUtils.sha256Hex(password + "{" + username + "}"));
        return this;
    }

    public AccountBuilder address(String city, String postalCode, String street, String houseNumber, String boxNumber,
            String country) {
        Address address = new Address();
        address.setStreet(street);
        address.setCity(city);
        address.setHouseNumber(houseNumber);
        address.setPostalCode(postalCode);
        address.setBoxNumber(boxNumber);
        address.setCountry(country);

        this.entity.setAddress(address);
        return this;
    }

    public AccountBuilder roleWithPermissions(Role role, Permission... permissions) {
        this.entity.getRoles().add(role);

        for (Permission permission : permissions) {
            role.getPermissions().add(permission);
        }
        return this;
    }

    public AccountBuilder email(String email) {
        this.entity.setEmailAddress(email);
        return this;
    }

    public AccountBuilder name(String firstName, String lastName) {
        this.entity.setFirstName(firstName);
        this.entity.setLastName(lastName);
        return this;
    }

    @Override
    Account assembleProduct() {
        return this.entity;
    }
}