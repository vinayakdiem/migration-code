package com.diemlife.controller;

import com.diemlife.constants.ScetConstants;
import com.diemlife.dao.SecurityRoleHome;
import com.diemlife.dao.UserHome;
import com.diemlife.forms.AdminForm;
import com.diemlife.forms.RoleSelect;
import com.diemlife.models.SecurityRole;
import com.diemlife.models.User;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import providers.MyUsernamePasswordAuthProvider;
import providers.MyUsernamePasswordAuthUser;
import security.JwtSessionLogin;
import services.UserProvider;
import views.html.admin.users.userForm;
import views.html.admin.users.userList;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Admin extends Controller {

    public static final String USER_ROLE = "admin";

    //private final PlayAuthenticate auth;

    private final UserProvider userProvider;

    private final MyUsernamePasswordAuthProvider userPaswAuthProvider;

    //private final MessagesApi msg;

    private final JPAApi jpaApi;
    private final FormFactory formFactory;

    @Inject
    public Admin(final UserProvider userProvider,
                 final MyUsernamePasswordAuthProvider userPaswAuthProvider,
                 final JPAApi api, FormFactory factory) {
        //this.auth = auth;
        this.userProvider = userProvider;
        this.userPaswAuthProvider = userPaswAuthProvider;
        //this.PASSWORD_RESET_FORM = formFactory.form(PasswordReset.class);
        //this.FORGOT_PASSWORD_FORM = formFactory.form(MyIdentity.class);

        //this.msg = msg;

        this.jpaApi = api;
        this.formFactory = factory;

    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result indexAuth() {
        // com.feth.play.module.pa.controllers.Authenticate.noCache(response());
        User user = this.userProvider.getUser(session());

        //return ok(indexAuth.render(user));
        return ok();
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result manageUsers() {
        // com.feth.play.module.pa.controllers.Authenticate.noCache(response());
        User user = this.userProvider.getUser(session());

        Query query = this.jpaApi.em().createQuery("SELECT u FROM User u");
        @SuppressWarnings("unchecked")
        List<User> users = query.getResultList();

        return ok(userList.render(users, user));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result createUser() {

        EntityManager em = this.jpaApi.em();

        User user = this.userProvider.getUser(session());

        Form<AdminForm> form = this.formFactory.form(AdminForm.class);

        RoleSelect roleSelect = new RoleSelect(em);
        List<lib.BootstrapSelectKeyValue> roles = roleSelect
                .populateVenueSelect();

        if (ScetConstants.POST.equals(request().method())) {
            form = form.bindFromRequest();

            List<Integer> rolesSelected = new ArrayList<Integer>();

            for (String key : form.data().keySet()) {
                if (key.contains("roles[")) {
                    String val = form.data().get(key);
                    try {
                        rolesSelected.add(Integer.parseInt(val));
                    } catch (NumberFormatException e) {
                        flash("error", val + " is not a valid selection.");
                        //em.close();
                        return badRequest(userForm.render(form, roles, user));
                    }
                }
            }

            roles = roleSelect.updateVenueSelect(rolesSelected, roles);

            if (form.hasErrors()) {
                //em.close();
                return ok(userForm.render(form, roles, user));
            } else {
                AdminForm input = form.get();

                MyUsernamePasswordAuthUser authUser = new MyUsernamePasswordAuthUser(input);

                UserHome userDao = new UserHome();

                User u = userDao.findByAuthUserIdentity(authUser, em);

                if (u != null) {
                    if (u.getEmailValidated()) {
                        // This user exists, has its email validated and is
                        // active
                        flash("error",
                                "This user exists, has its email validated and is active");
                        //em.close();
                        return badRequest(userForm.render(form, roles, user));
                    } else {
                        // this user exists, is active but has not yet validated
                        // its
                        // email
                        flash("error",
                                "This user exists, is active but has not yet validated its email");
                        //em.close();
                        return badRequest(userForm.render(form, roles, user));
                    }
                }

                User newUser = userDao.create(authUser, em);

                newUser.setFirstName(input.getFirstName());
                newUser.setLastName(input.getLastName());

                SecurityRoleHome roleDao = new SecurityRoleHome();

                Set<SecurityRole> rolesDb = roleDao.findByIds(input.getRoles(),
                        em);

                newUser.setSecurityRoles(rolesDb);

                newUser = userDao.merge(newUser, em);


                MyUsernamePasswordAuthProvider provider = this.userPaswAuthProvider;

                provider.sendPasswordResetMailing(newUser, ctx());

                //em.close();
                //return redirect(controllers.routes.Admin.manageUsers());
                return ok();
            }
        }

        //em.close();
        return ok(userForm.render(form, roles, user));

    }
}