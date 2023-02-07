package com.diemlife.dao;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.typesafe.config.Config;
import java.util.List;
import com.diemlife.models.S3File;
import org.apache.commons.io.FilenameUtils;
//FIXME Vinayak
//import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Repository;

import play.Logger;
import com.diemlife.plugins.S3Plugin;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;


import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Created by andrewcoleman on 6/14/16.
 */
@Repository
public class S3FileHome {

	@PersistenceContext
	EntityManager entityManager;
	
    public static final String METADATA_IMG_WIDTH = "img-width";
    public static final String METADATA_IMG_HEIGHT = "img-height";

    private Config config;

    @Inject
    public S3FileHome(Config config) {
        this.config = config;
    }

    public void persist(S3File transientInstance) {
        try {
            entityManager.persist(transientInstance);
        } catch (RuntimeException e) {
            Logger.error("S3FileHome :: persist : Error persisting file => " + e, e);
        }
    }

    public void remove(S3File persistentInstance) {
        try {
            entityManager.remove(persistentInstance);
        } catch (RuntimeException e) {
            Logger.error("S3FileHome :: remove : Error removing file => " + e, e);
        }
    }

    public S3File merge(S3File detachedInstance) {
        try {
            return entityManager.merge(detachedInstance);
        } catch (RuntimeException e) {
            Logger.error("S3FileHome :: merge : Error merging file => " + e, e);
        }
        return null;
    }

    public URL getUrl(S3File s3File, boolean isPhoto) throws MalformedURLException {
	    //FIXME Vinayak
    	return new URL("https://assets.diem.life/");
//        if (isPhoto) {
//            return new URL("https://assets.diem.life/" + s3File.getName());
//        }
//        return new URL("https://s3.amazonaws.com/" + S3Plugin.s3Bucket + "/" + s3File.getName());
    }

    private String generateFileName(String contentType) {
    	//FIXME Vinayak
        String extension = "";//StringUtils.substringAfterLast(contentType, "/");
        if (extension == null || extension.isEmpty()) {
            extension = contentType; 
        }
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSSS");
        final String date = simpleDateFormat.format(new Date());

        final String fileFormat = "%s/diemlife-%s.%s";

        if (config.getString("application.mode").equalsIgnoreCase("PROD")) {
            return format(fileFormat, "PROD", date, extension);
        }

        return format(fileFormat, "DEV", date, extension);
    }

    public void saveUserMedia(List<S3File> s3FileList) {
        save(s3FileList, null);
    }

    @Deprecated
    public void resaveUserMedia(List<S3File> s3FileList, String filename) {
        save(s3FileList, filename);
    }

    // Note: filename param is for resave case which is obsolete I believe; should just be null
    private void save(List<S3File> s3FileList, String filename) {
        checkNotNull(s3FileList, "s3File");
        final String bucket = S3Plugin.s3Bucket;

        // Save entries in DB
        saveFile(bucket, s3FileList, filename);

        // Put the files in S3
        for (S3File s3File : s3FileList) {
            final PutObjectRequest putObjectRequest;
            File file;
          //FIXME Vinayak
//            if ((file = s3File.getFile()) == null) {
//                final ObjectMetadata metadata = new ObjectMetadata();
//                metadata.setContentLength(s3File.getContentLength());
//                metadata.setCacheControl("max-age=31536000");
//                Integer imgWidth = s3File.getImgWidth();
//                Integer imgHeight = s3File.getImgHeight();
//                if ((imgWidth != null) && (imgHeight != null)) {
//                    metadata.addUserMetadata(METADATA_IMG_WIDTH, imgWidth.toString());
//                    metadata.addUserMetadata(METADATA_IMG_HEIGHT, imgHeight.toString());
//                }
//                putObjectRequest = new PutObjectRequest(bucket, s3File.getName(), s3File.getContentData(), metadata)
//                        .withCannedAcl(CannedAccessControlList.PublicRead);
//            } else {
//                final ObjectMetadata metadata = new ObjectMetadata();
//                metadata.setContentLength(s3File.getContentLength());
//                metadata.setCacheControl("max-age=31536000");
//                Integer imgWidth = s3File.getImgWidth();
//                Integer imgHeight = s3File.getImgHeight();
//                if ((imgWidth != null) && (imgHeight != null)) {
//                    metadata.addUserMetadata(METADATA_IMG_WIDTH, imgWidth.toString());
//                    metadata.addUserMetadata(METADATA_IMG_HEIGHT, imgHeight.toString());
//                }
//                putObjectRequest = new PutObjectRequest(bucket, s3File.getName(), file)
//                        .withCannedAcl(CannedAccessControlList.PublicRead)
//                        .withMetadata(metadata);
//            }
//            try {
//                S3Plugin.amazonS3.putObject(putObjectRequest);
//            } catch (Exception e) {
//                Logger.error("Could not putObject in S3 bucket. error saving. => " + e.getMessage(), e);
//            }
        }
    }

    public void delete(Long id) {
        checkNotNull(S3Plugin.amazonS3, "S3Plugin.amazon3");
        final String bucket = S3Plugin.s3Bucket;
        S3File s3File = findById(id);

        if (s3File != null) {
            // Extract the contentType from our db record
        	//FIXME Vinayak
//            S3Plugin.amazonS3.deleteObject(bucket, generateFileName(FilenameUtils.getExtension(s3File.getName())));

            this.remove(s3File);
        }
    }

    public S3File findById(Long id) {
        try {
            return entityManager.find(S3File.class, id);
        } catch (Exception e) {
            Logger.error(format("unable to find s3file by id: %s, %s", id, e));
            return null;
        }
    }

    public S3File findByUrl(String filename) {
        S3File s3File;
        try {
            s3File = entityManager.createQuery("SELECT s FROM S3File s WHERE s.name = :filename", S3File.class)
                    .setParameter("filename", filename)
                    .getSingleResult();
        } catch (Exception e) {
            Logger.error(format("unable to find s3file by id: %s, %s", filename, e));
            return null;
        }
        return s3File;
    }

    public List<S3File> findAll() {
        List<S3File> allMedia;
        try {
            allMedia = entityManager.createQuery("SELECT s FROM S3File s", S3File.class)
                    .getResultList();
        } catch (Exception e) {
            Logger.error(format("unable to find s3file by id: %s", e));
            return null;
        }
        return allMedia;
    }

    public boolean doesMediaHaveDifferentDimensions(S3File file) {
    	//FIXME Vinayak
        String filename = "";// file.getName();
        try {
            List<S3File> mediaList = entityManager
                    .createQuery("SELECT s FROM S3File s WHERE s.name LIKE :filename ", S3File.class)
                    .setParameter("filename", filename)
                    .getResultList();

            if (mediaList.size() == 4) {
                return true;
            }
        } catch (Exception e) {
            Logger.error(format("unable to find s3file by id: %s, %s", file.id, e));
        }
        return false;
    }

    // Saves record of file to database, not S3
    private void saveFile(String bucket, List<S3File> filesExtensions, String name) {
        String base;

        // Note: name param is for the resave case and I think it's obsolete.

        // All of this logic to stip off file extensions then add them back was very confusing and needed comments.  Have attempted to add comments
        // to make things clearer.
        
        // All of the files in this list should have the same extension
        //FIXME Vinayak
        String contentType = ""; //filesExtensions.get(0).getContentType();

        // Strip off the file extension and save it for use below
        String extension = FilenameUtils.getExtension(generateFileName(contentType));

        if (name != null) {
            // Resave image case
            if (config.getString("application.mode").equalsIgnoreCase("PROD")) {
                base = FilenameUtils.removeExtension(name.replace("https://assets.diem.life/PROD/", ""));
            } else {
                base = FilenameUtils.removeExtension(name.replace("https://assets.diem.life/DEV/", ""));
            }
        } else {
            // New image case -- just get the full name without a file extension
            base = FilenameUtils.removeExtension(generateFileName(contentType));
        }

      //FIXME Vinayak
//        for (S3File file : filesExtensions) {
//            file.setBucket(bucket);
//
//            // name is like _{small, medium, large}, this field would better be called "size variant" or something.  If you look in S3 bucket
//            // where these images are saved, you'll see foo_small.jpeg, foo_medium.jpeg, etc.
//            String filename = file.getName();
//
//            // Remove the extension from the size variant (if there is actually one attached to it)
//            String imageName = FilenameUtils.removeExtension(filename);
//
//            // Take the base calculated above, append the size variant, then append the extension.
//            String result = base + imageName + "." + extension;
//
//            // Overwrite the size variant name with the final, full name
//            file.setName(result);
//
//            // Save a record of S3 file to DB; the actual image data will be saved in S3.
//            this.persist(file);
//        }
    }
}
