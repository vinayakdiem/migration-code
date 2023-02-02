package com.diemlife.controller;

import play.*;
import com.diemlife.models.*;
import play.mvc.*;
import play.mvc.Http.*;

/**
 * Created by andrewcoleman on 3/20/16.
 */
public class Secured extends Security.Authenticator {

    public String getUsername(Context ctx) {
        return ctx.session().get("email");
    }

    public Result onUnauthorized(Context ctx) {
        //return redirect(routes.Application.login());
        return ok();
    }
}
