package com.diemlife.controller;

import com.diemlife.dto.BrandConfigDTO;
import com.diemlife.dto.BrandConfigImportDTO;
import com.diemlife.models.User;
import play.Logger;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.Request;
import play.mvc.Result;
import com.diemlife.security.JwtSessionLogin;
import com.diemlife.services.BrandConfigService;
import com.diemlife.services.UserProvider;
import com.diemlife.utils.CsvUtils;
import com.diemlife.utils.EndpointSecurityUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class BrandConfigController extends Controller {

    private final BrandConfigService brandConfigService;
    private final UserProvider userProvider;
    private final JPAApi jpaApi;

    @Inject
    public BrandConfigController(final BrandConfigService brandConfigService,
                                 final UserProvider userProvider,
                                 final JPAApi jpaApi) {
        this.brandConfigService = brandConfigService;
        this.userProvider = userProvider;
        this.jpaApi = jpaApi;
    }

    @JwtSessionLogin(required = true)
    public Result importBrandConfigCsv() {
        final User user = jpaApi.withTransaction(() -> userProvider.getUser(session()));
        if (EndpointSecurityUtils.isUserAdmin(user)) {
            Logger.info(format("Brand config import requested by admin user '%s'", requireNonNull(user).getEmail()));
        } else {
            return forbidden();
        }

        final Request request = request();
        final MultipartFormData<File> body = request.body().asMultipartFormData();
        final MultipartFormData.FilePart<File> csv = body.getFile("csv");
        try (final FileInputStream inputStream = new FileInputStream(csv.getFile())) {
            final List<BrandConfigImportDTO> brandConfigs = CsvUtils.readCvsFromStream(BrandConfigImportDTO.class, inputStream, UTF_8);

            brandConfigs.forEach(brandConfig -> {
                brandConfig.setIp(request.remoteAddress());
                brandConfig.setAgent(request.header(USER_AGENT).orElse(null));
                jpaApi.withTransaction(() -> brandConfigService.processBrandConfigImport(brandConfig));
            });

            Logger.info(format("Brand config successfully imported by admin user '%s'", requireNonNull(user).getEmail()));

            return ok();
        } catch (final IOException e) {
            Logger.error("Error reading CSV file from request", e);

            return internalServerError();
        }
    }

    @Transactional(readOnly = true)
    public Result getBrandConfigsForQuest(final Integer questId) {
        final List<BrandConfigDTO> configs = brandConfigService.getBrandConfigsForQuest(questId)
                .stream()
                .map(config -> BrandConfigDTO.builder()
                        .id(config.getBrandConfig().getUserId())
                        .name(config.getBrandConfig().getFullName())
                        .nonProfit(config.getBrandConfig().isNonProfit())
                        .logoUrl(config.getBrandConfig().getLogoUrl())
                        .siteUrl(config.getBrandConfig().getSiteUrl())
                        .secondaryRecipient(config.isSecondaryRecipient())
                        .build())
                .collect(toList());

        return ok(Json.toJson(configs));
    }

}
