package com.diemlife.services;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.diemlife.constants.ImageDimensions;
import com.diemlife.constants.ImageType;
import com.diemlife.models.S3File;
import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import play.Logger;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.diemlife.utils.ImageUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.StringUtils.hasText;

/**
 * A component used for resizing images prior to uploading for storage
 */
@Service
public class ImageProcessingService {

	@Autowired
    private FormFactory formFactory;

    public static class ImageResult {
        private byte[] data;
        private int width;
        private int height;

        public ImageResult(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }

        public byte[] getData() {
            return data;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    public List<S3File> getFileFromRequest(final Http.Request request, final String formParameterName, ImageType imageType) {
       
    	return getFileFromRequest(request, formParameterName, imageType, null);
    }

    public List<S3File> getFileFromRequest(final Http.Request request, final String formParameterName, ImageType imageType, String contentType) {

    	
    
        S3File s3File;
        if ("base64".equalsIgnoreCase(request.getQueryString("format")) || "base64".equalsIgnoreCase(String.valueOf(request.header("format"))) || (String.valueOf(request.header("format"))).indexOf("base64")>=0) {

            // This case should normally have a content type passed in.  If not, we likely missed a front end spot and default to jpeg.
            if ((contentType == null) || contentType.isEmpty()) {
                Logger.warn("getFileFromRequest - contentType is missing for base64 image upload.");
            }

            String convertedContentType = ImageUtils.contentTypeToImageIoType(contentType);

            final DynamicForm requestData = formFactory.form().bindFromRequest(request);
            final String base64Content = requestData.get(formParameterName);
            List<S3File> differentDimensionsImages = new ArrayList<>();
            if (isNotBlank(base64Content)) {
                
                // Create an image size copy for each desired dimenion variant
                for (ImageDimensions.Dimensions dimension : ImageDimensions.Dimensions.values()) {
                    s3File = new S3File();
                    byte[] content = Base64.getDecoder().decode(base64Content.getBytes(StandardCharsets.UTF_8));
                    ImageDimensions imageDimension = ImageDimensions.valueOf(imageType.name().concat(dimension.name()));
                    
                    ImageResult imgRes;
                    if ((imgRes = resizeByteArray(content, imageDimension, convertedContentType)) == null) {
                        return null;
                    }
                    content = imgRes.getData();

                  //FIXME Vinayak
//                    String dimensionName = dimension.isAddSuffix() ? dimension.name().toLowerCase() : "";
//                    s3File.setName(dimensionName);
//                    s3File.setContentData(new ByteArrayInputStream(content));
//                    s3File.setContentLength(content.length);
//                    s3File.setImgWidth(imgRes.getWidth());
//                    s3File.setImgHeight(imgRes.getHeight());
//                    s3File.setContentType(convertedContentType);

//                    Logger.debug("getFileFromRequest - adding variant: " + s3File.getName());

                    differentDimensionsImages.add(s3File);
                }

                Logger.debug("getFileFromRequest - extracted uploaded data in Base64 format with request parameter " + formParameterName);

                return differentDimensionsImages;
            }
        } else {
            final Http.MultipartFormData<File> body = request.body().asMultipartFormData();
            final Http.MultipartFormData.FilePart<File> fileContent = body.getFile(formParameterName);
            if (fileContent != null && fileContent.getFile() != null) {

                // This scenario may not have a content type explicitly passed in to the method, so grab it off of the request.  Again, if nothing found it
                // defaults to jpeg.
                String _contentType = fileContent.getContentType();
                if ((contentType != null) && !contentType.equals(_contentType)) {
                    Logger.warn("getFileFromRequest - content types differ for binary upload. explict: " + contentType + ", implicit: " + _contentType);
                }
                String convertedContentType = ImageUtils.contentTypeToImageIoType((contentType == null) ? fileContent.getContentType() : contentType);
                
                List<S3File> differentDimensionsImages = new ArrayList<>();

                // Create an image size copy for each desired dimenion variant
                for (ImageDimensions.Dimensions dimension : ImageDimensions.Dimensions.values()) {
                    s3File = new S3File();
                    ImageDimensions imageDimension = ImageDimensions.valueOf(imageType.name().concat(dimension.name()));
                    
                    ImageResult imgRes;
                    if ((imgRes = processImage(fileContent.getFile(), imageDimension, convertedContentType)) == null) {
                        return null;
                    }
                    final byte[] base64Content = imgRes.getData();

                    //String imageName = FilenameUtils.removeExtension(fileContent.getFilename());
                  //FIXME Vinayak
//                    String dimensionName = dimension.isAddSuffix() ? dimension.name().toLowerCase() : "";
//                    String extension = FilenameUtils.getExtension(fileContent.getFilename());
//
//                    s3File.setName(dimensionName + "." + extension);
//                    s3File.setContentData(new ByteArrayInputStream(base64Content));
//                    s3File.setContentLength(base64Content.length);
//                    s3File.setImgWidth(imgRes.getWidth());
//                    s3File.setImgHeight(imgRes.getHeight());
//                    s3File.setContentType(convertedContentType);
//
//                    Logger.debug("getFileFromRequest - adding variant: " + s3File.getName());

                    differentDimensionsImages.add(s3File);
                }

                Logger.debug("getFileFromRequest - extracted uploaded data in binary format with request parameter " + formParameterName);

                return differentDimensionsImages;
            }
        }

        Logger.warn("getFileFromRequest - Unable to extract uploaded data from request with parameter " + formParameterName);

        return null;
    }

    public ImageResult processImage(File file, ImageDimensions imageDimension, String contentType) {
    	//FIXME Vinayak
//        try {
//            return from(file, imageDimension, contentType);
//        } catch (ImageProcessingException | IOException e) {
//            Logger.error("processImage - Unable to process image file: " + file.getAbsolutePath() + ", " + e.toString());
            return null;
//        }
    }

    private ImageResult from(File file, ImageDimensions imageDimension, String contentType) { 
    		
    	//FIXME Vinayak
//    	throws IOException, ImageProcessingException {
//        BufferedImage image = ImageIO.read(file);
//        if (image == null) {
//            Logger.error("from - image read returned null, this likely not an image file: " + file.getAbsolutePath());
//            return null;
//        }

      //FIXME Vinayak
//        image = resizeAndFormatImage(image, file, imageDimension);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        
        String convertedContentType = ImageUtils.contentTypeToImageIoType(contentType);
        Logger.debug("from - writing image as type: " + convertedContentType);

      //FIXME Vinayak
//        ImageIO.write(image, convertedContentType, byteArrayOutputStream);

//        byteArrayOutputStream.flush();
        byte[] bytes = byteArrayOutputStream.toByteArray();
//        byteArrayOutputStream.close();


       return null;
      //FIXME Vinayak
//        return new ImageResult(bytes, image.getWidth(), image.getHeight());
    }

    public ImageResult resizeByteArray(byte[] content, ImageDimensions imageDimension, String contentType) {
        try {
            BufferedImage imageFromBytes = ImageIO.read(new ByteArrayInputStream(content));
            if (imageFromBytes == null) {
                Logger.error("resizeByteArray - couldn't read image.");
                return null;
            }

            Logger.debug("resizeByteArray - type before resize: " + ImageUtils.getImageTypeString(imageFromBytes));

          //FIXME Vinayak
//            BufferedImage bufferedImage = Scalr.resize(ImageIO.read(new ByteArrayInputStream(content)),
//                    Scalr.Method.QUALITY,
//                    imageDimension.getWidth(), imageDimension.getHeight());

//            Logger.debug("resizeByteArray - type after resize: " + ImageUtils.getImageTypeString(bufferedImage));
//
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // Note: this was previously assuming a jpeg always.  I pray other things don't depend on us always saving a jpeg in the S3
            String convertedContentType = ImageUtils.contentTypeToImageIoType(contentType);
            Logger.debug("resizeByteArray - writing img as type: " + convertedContentType);
          //FIXME Vinayak
//            ImageIO.write(bufferedImage, convertedContentType, byteArrayOutputStream);
//
//            byteArrayOutputStream.flush();
//            byte[] bytes = byteArrayOutputStream.toByteArray();
//            byteArrayOutputStream.close();
//
//            return new ImageResult(bytes, bufferedImage.getWidth(), bufferedImage.getHeight());
            return null;
        } catch (IOException e) {
            Logger.error("resizeByteArray - couldn't process image: " + e.toString());
            return null;
        }
    }

    private BufferedImage resizeAndFormatImage(BufferedImage image, File file, ImageDimensions imageDimension) {
  //FIXME Vinayak
//    		throws IOException, ImageProcessingException {
        
        Logger.debug("resizeAndFormatImage - type before resize: " + ImageUtils.getImageTypeString(image));

        //scaling the image to be IMAGE_SIZE x IMAGE_SIZE
      //FIXME Vinayak
//        BufferedImage scaledImage = Scalr.resize(image,
//                Scalr.Method.QUALITY,
//                imageDimension.getWidth(), imageDimension.getHeight());
//
//        Logger.debug("resizeAndFormatImage - type after resize: " + ImageUtils.getImageTypeString(scaledImage));
//
//        Metadata metadata = ImageMetadataReader.readMetadata(file);
//        ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

        int orientation = 0;
        try {
        	//FIXME Vinayak
//            orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (Exception ex) {
            Logger.warn("no EXIF information found for image: {}", file.getName());
        }

      //FIXME Vinayak
//        switch (orientation) {
//            case 1:
//                break;
//            case 2: // Flip X
//                scaledImage = Scalr.rotate(scaledImage, Scalr.Rotation.FLIP_HORZ);
//                break;
//            case 3: // PI rotation
//                scaledImage = Scalr.rotate(scaledImage, Scalr.Rotation.CW_180);
//                break;
//            case 4: // Flip Y
//                scaledImage = Scalr.rotate(scaledImage, Scalr.Rotation.FLIP_VERT);
//                break;
//            case 5: // - PI/2 and Flip X
//                scaledImage = Scalr.rotate(scaledImage, Scalr.Rotation.CW_90);
//                scaledImage = Scalr.rotate(scaledImage, Scalr.Rotation.FLIP_HORZ);
//                break;
//            case 6: // -PI/2 and -width
//                scaledImage = Scalr.rotate(scaledImage, Scalr.Rotation.CW_90);
//                break;
//            case 7: // PI/2 and Flip
//                scaledImage = Scalr.rotate(scaledImage, Scalr.Rotation.CW_90);
//                scaledImage = Scalr.rotate(scaledImage, Scalr.Rotation.FLIP_VERT);
//                break;
//            case 8: // PI / 2
//                scaledImage = Scalr.rotate(scaledImage, Scalr.Rotation.CW_270);
//                break;
//            default:
//                break;
//        }

      //FIXME Vinayak
        return null;
//        return scaledImage;
    }

}
