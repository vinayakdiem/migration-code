package dao;

import models.Brand;
import models.CompanyRepresentative;
import models.CompanyRole;
import models.User;
import play.db.jpa.JPAApi;

import javax.persistence.EntityManager;

public class CompanyRoleDAO extends TypedSingletonDAO<CompanyRole> {
    public CompanyRoleDAO(JPAApi jpaApi) {
        super(jpaApi);
    }

    public CompanyRole createRole(Brand company, User user, String role) {
        final CompanyRole representative = new CompanyRole();
        representative.setCompany(company);
        representative.setUser(user);
        representative.setRole(role);

        return save(representative, CompanyRole.class);
    }

    public CompanyRole getCompanyRoleForUser(User user) {
        EntityManager em = jpaApi.em();
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
