package com.diemlife.controller;

import com.typesafe.config.Config;

import com.diemlife.dao.*;
import com.diemlife.dto.BrandDTO;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import forms.RegistrationForm;
import models.*;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.Database;
import play.db.NamedDatabase;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.diemlife.providers.MyUsernamePasswordAuthUser;
import com.diemlife.security.JwtSessionLogin;

import com.diemlife.services.UserProvider;

@JwtSessionLogin
public class BrandController extends Controller {

    private final Config config;
    private final UserProvider userProvider;
    private final FormFactory formFactory;
    private final JPAApi jpaApi;
    private Database db;
    private Database dbRo;
    public static final String REPRESENTATIVE_ROLE = "customer_representative";

    @Inject
    public BrandController(final Config config, final FormFactory formFactory, final JPAApi jpaApi, final UserProvider userProvider, 
            Database db, @NamedDatabase("ro") Database dbRo)
    {
        this.config = config;
        this.formFactory = formFactory;
        this.jpaApi = jpaApi;
        this.userProvider = userProvider;
        this.db = db;
        this.dbRo = dbRo;
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getBrand(final @NotNull String name) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        BrandDTO result = new BrandDTO();

        return ok(Json.toJson(result));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getBrandFromId(final @NotNull Long id) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        BrandDAO brandDao = new BrandDAO(jpaApi);
        Brand brand = brandDao.getBrandFromId(id);

        return ok(Json.toJson(brand));
    }

    @Transactional
    public Result createBrand() {
        DynamicForm form = this.formFactory.form().bindFromRequest();
        // Extract fields from form
        String name = form.get("name");
        String url = form.get("url");
        String type = form.get("type");
        String userId = form.get("user_id");
        String role = form.get("role");
        //String isRepresentative = form.get("is_representative");

        Brand result;
        BrandDAO brandDao = new BrandDAO(jpaApi);

        // TODO: define more types
        // ToDo: new: If companyRole schema changes to include is_representative, update createBrand method to include it
        result = brandDao.createBrand(name, type, url);

        // associate user to the brand id in company representative table
        CompanyRole companyRole;
        CompanyRoleDAO companyRoleDao = new CompanyRoleDAO(jpaApi);
        final EntityManager entityManager = this.jpaApi.em();

        //Find user which was created during first login step
        User user = UserHome.findById(Integer.parseInt(userId), entityManager);
        //Assign the company role as SuperAdmin if it is a business account.
        companyRole = companyRoleDao.createRole(result, user, role);

        if (companyRole == null) {
            return noContent();
        }

        return ok(Json.toJson(result));
    }

    @Transactional
    public Result updateAddressInBrand() {
        DynamicForm form = this.formFactory.form().bindFromRequest();
        String country = form.get("country");
        String addLine1 = form.get("addressLine1");
        String addLine2 = form.get("addressLine2");
        String city = form.get("city");
        String state = form.get("state");
        String zip = form.get("zip");
        String companyId = form.get("companyId");
        String phone = form.get("phone");

        Address address;
        AddressDAO addressDao = new AddressDAO(jpaApi);
        address = addressDao.createAddress(addLine1, addLine2, city, state, country, zip);

        // update address id in brand (Company table)
        BrandDAO brandDao = new BrandDAO(jpaApi);
        if (!brandDao.addAddressForBrand(address, Integer.parseInt(companyId))) {
            return noContent();
        }
        //Update phone_number field for brand (Company table)
        if (!brandDao.addPhoneForBrand(phone, Integer.parseInt(companyId))) {
            return noContent();
        }

        return ok(Json.toJson(address));
    }

    @Transactional
    public Result createCompanyRepresentative(){
        DynamicForm form = this.formFactory.form().bindFromRequest();
        /*String firstName = form.get("first_name");
        String lastName = form.get("last_name");*/
        String email = form.get("email");
        String role = form.get("role");
        String brand = form.get("brand");

        final EntityManager entityManager = this.jpaApi.em();
        User user = UserHome.findByEmail(email,entityManager);

        if(user!=null){
            CompanyRoleDAO companyRoleDAO= new CompanyRoleDAO(jpaApi);
            CompanyRole companyRole = companyRoleDAO.getCompanyRoleForUser(user);

            BrandDAO brandDao = new BrandDAO(jpaApi);
            Brand representativeCompany;

            if(companyRole==null) {
                //If User exists but there is no company role defined for the user then create the role.
                //Brand can't be null as there has to be a company created before calling the request for company representative.
                representativeCompany = brandDao.getBrand(brand);
                companyRole = companyRoleDAO.createRole(representativeCompany,user,role);
            }/*else if(companyRole.getIsRepresentative() != 1){
                // If the user already exists, then set the role as CR
                companyRole.setIsRepresentative(1);
                //companyRole.setRole(role);
                companyRole = companyRoleDAO.updateCompanyRoleForUser(companyRole);
            }*/

            CompanyRepresentativeDAO companyRepresentativeDAO = new CompanyRepresentativeDAO(jpaApi);
            //companyRepresentativeDAO.createCompanyRepresentative(firstName,lastName,email,user,companyRole.getCompany(),"");
            companyRepresentativeDAO.createCompanyRepresentative(email,user,companyRole.getCompany(),"");
        }

        //ToDo: Not possible to create user without password
        //RegistrationForm regForm = new RegistrationForm(firstName,lastName,email);
        //MyUsernamePasswordAuthUser authUser = new MyUsernamePasswordAuthUser(regForm);
        //User newUser = createUserByRole(authUser, null);


        return ok();
    }

    private User createUserByRole(MyUsernamePasswordAuthUser authUser, String tiUserId) {
        EntityManager em = this.jpaApi.em();
        UserHome userDao = new UserHome();

        User newUser = userDao.createByRole(authUser, controllers.BrandController.REPRESENTATIVE_ROLE, null, em);

        return newUser;

    }

    @Transactional
    public Result updateBrand(final @NotNull String name) {

        BrandDAO brandDao = new BrandDAO(jpaApi);

        // TODO: consider using reader here instead
        Brand brand = brandDao.getBrand(name);
        if (brand == null) {
            return badRequest();
        }

//TODO: Fix it
//            if(!brandDao.updateBrand(c, name, address1, address2, city, state, postalCode, country, phone)) {
//                return badRequest();
//            }

        return ok();
    } 
}
