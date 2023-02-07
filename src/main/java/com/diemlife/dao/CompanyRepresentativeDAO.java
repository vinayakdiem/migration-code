package com.diemlife.dao;

import org.springframework.stereotype.Repository;

import com.diemlife.models.Brand;
import com.diemlife.models.CompanyRepresentative;
import com.diemlife.models.User;

@Repository
public class CompanyRepresentativeDAO extends TypedSingletonDAO<CompanyRepresentative>{
    

    public CompanyRepresentative createCompanyRepresentative(String email, User user, Brand company, String stripeCustomerId){
        final CompanyRepresentative companyRepresentative = new CompanyRepresentative();
      //FIXME Vinayak
//        companyRepresentative.setEmail(email);
//        companyRepresentative.setCompany(company);
//        companyRepresentative.setUser(user);
//        companyRepresentative.setStripeCustomerId(stripeCustomerId);

        return save(companyRepresentative,CompanyRepresentative.class);
    }

}
