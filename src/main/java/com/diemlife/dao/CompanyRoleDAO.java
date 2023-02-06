package com.diemlife.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.models.Brand;
import com.diemlife.models.CompanyRole;
import com.diemlife.models.User;


@Repository
public class CompanyRoleDAO extends TypedSingletonDAO<CompanyRole> {
   
	@PersistenceContext
	private EntityManager em;
	
    public CompanyRole createRole(Brand company, User user, String role) {
        final CompanyRole representative = new CompanyRole();
        representative.setCompany(company);
        representative.setUser(user);
        representative.setRole(role);

        return save(representative, CompanyRole.class);
    }

    public CompanyRole getCompanyRoleForUser(User user) {
        return em.createQuery("Select c from CompanyRole c where c.user = :user", CompanyRole.class)
                .setParameter("user", user)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public CompanyRole updateCompanyRoleForUser(CompanyRole companyRole){
        return save(companyRole,CompanyRole.class);
    }
}
