package com.diemlife.dao;

import models.Brand;
import models.CompanyRepresentative;
import models.CompanyRole;
import models.User;
import play.db.jpa.JPAApi;

public class CompanyRepresentativeDAO extends TypedSingletonDAO<CompanyRepresentative>{
    public CompanyRepresentativeDAO(JPAApi jpaApi) {
        super(jpaApi);
    }

    public CompanyRepresentative createCompanyRepresentative(String email, User user, Brand company, String stripeCustomerId){
        final CompanyRepresentative companyRepresentative = new CompanyRepresentative();
        companyRepresentative.setEmail(email);
        companyRepresentative.setCompany(company);
        companyRepresentative.setUser(user);
        companyRepresentative.setStripeCustomerId(stripeCustomerId);

        return save(companyRepresentative,CompanyRepresentative.class);
    }

}
