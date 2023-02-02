package com.diemlife.controller;

import com.fasterxml.jackson.databind.node.TextNode;
import com.typesafe.config.Config;
import com.diemlife.constants.ImageDimensions;
import com.diemlife.constants.ImageType;
import com.diemlife.dao.S3FileHome;
import java.util.ArrayList;
import java.util.List;
import com.diemlife.models.S3File;
import com.diemlife.models.User;
import play.Logger;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import security.JwtSessionLogin;
import services.ImageProcessingService;
import services.UserProvider;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.stream.Stream;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Controller that deals with requests working with images
 */
public class ImageProcessingController extends Controller {

    private final ImageProcessingService imageProcessingService;
    private final UserProvider userProvider;
    private final Config config;
    private final JPAApi jpaApi;

    @Inject
    public ImageProcessingController(final ImageProcessingService imageProcessingService,
                                     final UserProvider userProvider,
                                     final Config config,
                                     final JPAApi jpaApi) {
        this.imageProcessingService = checkNotNull(imageProcessingService, "imageProcessingService");
        this.userProvider = userProvider;
        this.config = config;
        this.jpaApi = jpaApi;
    }

    public Result processImagePreview() {
        Logger.debug("processImagePreview");

        final Http.MultipartFormData<File> body = request().body().asMultipartFormData();
        final Http.MultipartFormData.FilePart<File> fileContent = body.getFile("questImage");
        String contentType = fileContent.getContentType();

        Logger.debug("processImagePreview - " + contentType);

        ImageProcessingService.ImageResult imgRes;
        if ((imgRes = imageProcessingService.processImage(fileContent.getFile(), ImageDimensions.QUEST_IMAGE_ORIGINAL, contentType)) == null) {
            return internalServerError("Unable to process image for preview.");
        }
        return ok(TextNode.valueOf(Base64.getEncoder().encodeToString(imgRes.getData())));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result uploadImage() {
        Logger.debug("uploadImage");

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        final Http.Request request = request();

        // TODO: how is this code better than an old-fashioned switch-case?
        final ImageType imageType = Stream.of(ImageType.values())
                .filter(value -> request.header("Image-Type").filter(header -> value.name().equals(header)).isPresent())
                .findFirst()
                .orElse(ImageType.QUEST_IMAGE);

        Logger.debug("uploadImage - " + imageType.toString());

        List<S3File> questImageDimensionsList = imageProcessingService.getFileFromRequest(request, "imageFile", imageType);
        if (questImageDimensionsList == null || questImageDimensionsList.isEmpty()) {
            return internalServerError("Unable to process image.");
        }

        final S3FileHome s3FileHome = new S3FileHome(config);
        final EntityManager em = jpaApi.em();
        List<URL> urlList = new ArrayList<>();
        s3FileHome.saveUserMedia(questImageDimensionsList, em);
        for (S3File questImage : questImageDimensionsList) {
            try {
                final URL url = s3FileHome.getUrl(questImage, true);
                urlList.add(url);
            } catch (final MalformedURLException e) {
                Logger.error(e.getMessage(), e);
                return internalServerError();
            }
        }
        urlList.forEach(URL::toExternalForm);
        return ok(TextNode.valueOf(urlList.get(0).toExternalForm()));
    }

}
