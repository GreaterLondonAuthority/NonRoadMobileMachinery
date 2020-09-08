/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.pivotal.system.hibernate.entities.MediaEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.Constants;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles requests for storing and managing Media items
 */
@Authorise
@Controller
@RequestMapping(value = {"/media"})
public class MediaController extends AbstractAdminController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MediaController.class);
    /** Constant <code>SYSTEM_IMAGE_FOLDER="/themes/default/imgs/"</code> */
    public static final String SYSTEM_IMAGE_FOLDER = "/themes/default/imgs/";

    /**
     * Manages the steaming of an image to the output
     *
     * @param response Response to send the image to
     * @param filename Temporary file to send
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    @Authorise(notLoggedIn = true)
    @RequestMapping(value = "/stream/tmp")
    public void getTemporaryFile(HttpServletRequest request, HttpServletResponse response, @RequestParam(value="f") String filename) {

        File file = new File(Common.getTemporaryDirectory() + File.separator + filename);

        // Check that we are allowed to do this

        //if (!Constants.isLoopbackRequest(request) && !Common.doStringsMatch(request.getServerName(), "localhost"))
        if (!Constants.isLoopbackRequest(request) && !Common.doStringsMatch(request.getServerName(), "localhost"))
            sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed");

        // Check the file exists and if not send an error

        else if (!file.exists())
            sendError(HttpServletResponse.SC_NOT_FOUND, "File is not known");

        // Send the file back

        else {
            response.setContentType(ServletHelper.getServletContext().getMimeType(filename));
            response.setContentLength((int) file.length());
            try {
                Common.pipeInputToOutputStream(file, response.getOutputStream());
            }
            catch (Exception e) {
                sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Manages the streaming of the latest version of a plan as an image
     *
     * @param session  Session to use
     * @param response Response to send the image to
     * @param refid    ID of the referenced entity
     * @param refname  Name of the referenced entity
     */
    @RequestMapping(value = "/entstream/{refid}/{refname}")
    public void getStreamNormal(HttpSession session, HttpServletResponse response, @PathVariable Integer refid, @PathVariable String refname) {
//        LayoutPlanEntity plan = LayoutPlanManager.getInstance().getLatestEntity(refname, refid);
//        if (plan != null) {
//            getStream(session, response, plan.getImage().getId(), null);
//        }
    }

    /**
     * Manages the steaming of a media item to the output
     *
     * @param session Session to use
     * @param response Response to send the image to
     * @param id ID of the media entity
     */
    @RequestMapping(value = "/download/{id}")
    public void getDownload(HttpSession session, HttpServletResponse response, @PathVariable Integer id) {
        writeToStream(session, response, id, null, true);
    }

    /**
     * Manages the steaming of an image to the output
     *
     * @param session Session to use
     * @param response Response to send the image to
     * @param id ID of the media entity
     */
    @RequestMapping(value = "/stream/{id}")
    public void getStreamNormal(HttpSession session, HttpServletResponse response, @PathVariable Integer id) {
        getStream(session, response, id, null);
    }

    /**
     * Manages the steaming of an image to the output
     *
     * @param session   Session to use
     * @param response  Response to send the image to
     * @param type      Type of media link entity
     * @param id        ID of the media link entity
     */
    @RequestMapping(value = "/qr/{type}/{id}")
    public void getCaseQRCode(HttpSession session, HttpServletResponse response, @PathVariable String type, @PathVariable Integer id) {

        try {
            MediaEntity mediaEntity = null;
//            if ("case".equalsIgnoreCase(type)) {
//                CaseMediaEntity caseMedia = HibernateUtils.getEntity(CaseMediaEntity.class, id);
//                if (caseMedia != null) mediaEntity = caseMedia.getMedia();
//            }
//            else if("meeting".equalsIgnoreCase(type)) {
//                MeetingMediaEntity meetingMedia = HibernateUtils.getEntity(MeetingMediaEntity.class, id);
//                if (meetingMedia != null) mediaEntity = meetingMedia.getMedia();
//            }
            if (!Common.isBlank(mediaEntity)) {
                response.setContentType(session.getServletContext().getMimeType("test.png"));
                BitMatrix matrix = new MultiFormatWriter().encode( Constants.getAppPath() + "/media/download/" + mediaEntity.getId(), BarcodeFormat.QR_CODE, 200, 200);
                MatrixToImageWriter.writeToStream(matrix, "png", response.getOutputStream());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Manages the steaming of an image to the output at the desired resolution
     *
     * @param session Session to use
     * @param response Response to send the image to
     * @param id ID of the media entity
     * @param size Indicator of the size to return
     */
    @RequestMapping(value = "/stream/{id}/{size}")
    public void getStream(HttpSession session, HttpServletResponse response, @PathVariable Integer id, @PathVariable String size) {
        writeToStream(session, response, id, size, false);
    }

    /**
     * Manages the steaming of an image to the output at the desired resolution
     *
     * @param session Session to use
     * @param response Response to send the image to
     * @param id ID of the media entity
     * @param size Indicator of the size to return
     */
    public static void writeToStream(HttpSession session, HttpServletResponse response, @PathVariable Integer id, @PathVariable String size) {
        writeToStream(session, response, id, size, false);
    }

    /**
     * Manages the steaming of an image to the output at the desired resolution
     *
     * @param session Session to use
     * @param response Response to send the image to
     * @param id ID of the media entity
     * @param size Indicator of the size to return
     * @param download True if the media should be sent as an attachment
     */
    public static void writeToStream(HttpSession session, HttpServletResponse response, @PathVariable Integer id, @PathVariable String size, boolean download) {

        // Check we have something first

        String error = null;

        try {
            if (id==null)
                error = "The media ID is null";
            else {
                if (id<0) id=id*-1;
                MediaEntity media = HibernateUtils.getEntity(MediaEntity.class, id);
                if (media==null)
                    error = String.format("No media available for ID [%d]", id);
                else {

                    // Set the content if we can

                    if (Common.isBlank(media.getExtension())) {
                        response.setContentType("application/octet-stream");
                        logger.warn("Using content type [{}] for media [{} - {}] because extension is missing", response.getContentType(), id, media.getName());
                    }
                    else {
                        response.setContentType(session.getServletContext().getMimeType("test." + media.getExtension()));
                    }

                    // Determine what other headers to send

                    if (download) {
                        String filename = media.getName() + '.' + (Common.isBlank(media.getExtension())?".tmp":media.getExtension());
                        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + '"');
                        response.setHeader("Content-Description", filename);
                    }

                    if (!Common.isBlank(media.getFilename())) {

                        // Figure out which version of the file we want

                        String sizeAbbr = "";
                        String filename = media.getFilename();
                        if (Common.doStringsMatch(size, "16", "small", "s")) {
                            filename = filename.replace(".", "16.");
                            sizeAbbr = "16";
                        }
                        else if (Common.doStringsMatch(size, "64", "large", "l")) {
                            filename = filename.replace(".", "64.");
                            sizeAbbr = "64";
                        }
                        String externalStorage = HibernateUtils.getUploadedFileLocation();

                        File file = new File(externalStorage + filename);
                        if (!file.exists()) {
                            file = new File(ServletHelper.getRealPath(SYSTEM_IMAGE_FOLDER + filename));

                            // Check the file exists and if not, use a default

                            if (!file.exists()) {
                                file = new File(file.getParentFile().getAbsolutePath() + File.separator + String.format("default%s.png", sizeAbbr));
                                if (!file.exists()) {
                                    file = new File(file.getParentFile().getParentFile().getAbsolutePath() + File.separator + String.format("default%s.png", sizeAbbr));
                                    if (file.exists())
                                        logger.warn("Cannot find the icon for [{} ({})] - using default", media.getName(), id);
                                }
                            }
                        }

                        // Send the content

                        if (file.exists()) {
                            response.setContentLength((int) file.length());
                            Common.pipeInputToOutputStream(file, response.getOutputStream());
                        }
                        else {
                            error = String.format("No media file available for ID [%d] - [%s]", id, file.getAbsolutePath());
                        }
                    }
                }
            }

            // Send an error if something is wrong

            if (error!=null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, error);
            }
        }
        catch (Exception e) {
            logger.error("Cannot send an error to browser - {}", PivotalException.getErrorMessage(e));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        return MediaEntity.class;
    }

    /**
     * Returns a JSON list of all the images ordered by name and optionally
     * limited to only those images that are of the specified type
     *
     * @param type Type to limit the list to
     * @return a {@link java.util.List} object.
     */
    @RequestMapping(value = "/library/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List listImages(@RequestParam(value = "type", required = false) String type) {
        List<Map<String, Object>> returnValue=new ArrayList<>();
        List<Object[]> tmp;
        if (!Common.isBlank(type))
            tmp = HibernateUtils.selectEntities("select id,name,description,type,extension,fileSize,filename from MediaEntity where type like ? order by name", String.format("%%%s%%",type.replaceAll("/","")));
        else
            tmp = HibernateUtils.selectEntities("select id,name,description,type,extension,fileSize,filename from MediaEntity order by name");
        if (!Common.isBlank(tmp)) {
            for (Object[] aTmp : tmp) {
                Map<String, Object> row = new HashMap<>();
                row.put("internalid", ""+aTmp[0]);
                row.put("name", aTmp[1] + " [" + aTmp[0] + "]");
                row.put("description", aTmp[2]);
                row.put("mediatype", aTmp[3]);
                row.put("type", "f");
                row.put("extension", aTmp[4]);

                //If the file size is not available in the size field, let's try and get it from the a file in the image folder.
                String tmpSize = ""+aTmp[5];
                if(Common.isBlank(tmpSize) || tmpSize.equals("0")){
                    File file = new File(ServletHelper.getRealPath(SYSTEM_IMAGE_FOLDER + aTmp[6]));
                    if(file.exists()){
                        tmpSize = "" + file.length();
                    }
                }
                row.put("size", Common.parseInt(tmpSize));
                row.put("path", ""+aTmp[0]);
                returnValue.add(row);
            }
        }
        return returnValue;
    }

    /**
     * Returns a JSON list with the information of the image with the given id
     *
     * @param id a {@link java.lang.Integer} object.
     * @return a {@link java.util.List} object.
     */
    @RequestMapping(value = "/library/mediainfo/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List imageInfo(@PathVariable Integer id) {
        List<Map<String, Object>> returnValue=new ArrayList<>();
        List<Object[]> tmp;
        if (!Common.isBlank(id)) {
            tmp = HibernateUtils.selectEntities("select id,name,description,type,extension,fileSize from MediaEntity where id=?", id);

            if (!Common.isBlank(tmp)) {
                for (Object[] aTmp : tmp) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", aTmp[0]);
                    row.put("name", aTmp[1] + " [" + aTmp[0] + "]");
                    row.put("description", aTmp[2]);
                    row.put("type", "f");
                    row.put("mediatype", aTmp[3]);
                    row.put("extension", aTmp[4]);
                    row.put("size", aTmp[5]);
                    row.put("path", ""+aTmp[0]);
                    returnValue.add(row);
                }
            }
        }
        return returnValue;
    }


    /**
     * Handles all unmapped requests
     */
    @RequestMapping(value = "/**", method = RequestMethod.GET)
    public void show() {
    }


    /**
     * Returns the view for the image picker
     *
     * @return view name
     * @param model a {@link org.springframework.ui.Model} object.
     * @param inputFieldName a {@link java.lang.String} object.
     * @param type a {@link java.lang.String} object.
     * @param isReadOnly a {@link java.lang.Boolean} object.
     */
    @RequestMapping(value = {"/uploadfile"}, method = RequestMethod.GET)
    public String showUpload(Model model,
                             @RequestParam("inputname") String inputFieldName,
                             @RequestParam(value = "type", required = false) String type,
                             @RequestParam(value="isreadonly", required = false) Boolean isReadOnly) {

        model.addAttribute("InputField", inputFieldName);
        Boolean tmpBool = false;
        if(isReadOnly != null){
            tmpBool = isReadOnly;
        }
        model.addAttribute("IsReadOnly", tmpBool);

        String tmpType = MediaEntity.TYPE_CODE_ICON;

        if(!Common.isBlank(type)){
            tmpType = type;
        }
        model.addAttribute("type", tmpType);

        return "media/upload";
    }

    /**
     * Returns the view for the image picker
     *
     * @return view name
     * @param model a {@link org.springframework.ui.Model} object.
     * @param inputFieldName a {@link java.lang.String} object.
     * @param type a {@link java.lang.String} object.
     * @param isReadOnly a {@link java.lang.Boolean} object.
     */
    @RequestMapping(value = {"/picker"}, method = RequestMethod.GET)
    public String showPicker(Model model,
                             @RequestParam("inputname") String inputFieldName,
                             @RequestParam(value = "type", required = false) String type,
                             @RequestParam(value="isreadonly", required = false) Boolean isReadOnly) {

        model.addAttribute("InputField", inputFieldName);
        Boolean tmpBool = false;
        if(isReadOnly != null){
            tmpBool = isReadOnly;
        }
        model.addAttribute("IsReadOnly", tmpBool);

        String tmpType = MediaEntity.TYPE_CODE_ICON;

        if(!Common.isBlank(type)){
            tmpType = type;
        }
        model.addAttribute("type", tmpType);

        return "media/picker/default";
    }


    /**
     * Uploads an image from the media browser. The request payload consists of the uploaded file.
     *
     * @param type File type
     * @param file uploaded file
     * @return A source Object of the image browser
     */
    @RequestMapping(value = {"/uploadmedia"}, method = RequestMethod.POST)
    @ResponseBody
    public Map<String,String> uploadMedia(@RequestParam(value = "type", required = false) String type,
                                          @RequestParam("file") MultipartFile file) {
        logger.info("Uploading ");
        Map<String,String> res = new HashMap<>();

        MediaEntity me = HibernateUtils.getEntity(MediaEntity.class);
        try {
            me.setFilename(file.getOriginalFilename());
            me.setName(Common.getFilenameBody(file.getOriginalFilename()));
            me.setFileSize((int) file.getSize());
            me.setExtension(Common.getFilenameExtension(file.getOriginalFilename()));
            me.setType(Common.isBlank(type) ? "f" : type);
            me.setTimeModifiedNow();
            HibernateUtils.save(me);
            CaseManager.addMediaFile(me, file);
            res.put("id", "" + me.getId());
            res.put("internalid", ""+me.getId());
            res.put("name",me.getName()+" ["+me.getId() + "]");
            res.put("description",me.getDescription());
            res.put("type","f");
            res.put("type",""+me.getType());
            res.put("extension",me.getExtension());
            res.put("size",""+me.getFileSize());
            res.put("path",""+me.getId());
        }
        catch (Exception e) {
            logger.error("Failed to upload the image - ", e);
        }
        return res;
    }

    /**
     * Delete the media with the given id. Internal records are not deletable.
     * References to the media record in other entities will be set to null
     *
     * @param id Identifier of the media to delete
     * @return Json object with relevant information (errors)
     */
    @RequestMapping(value = {"/deletemedia"}, method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public JsonResponse deleteMedia(@RequestParam("id") String id) {
        logger.info("Deleting media");
        JsonResponse returnValue = new JsonResponse();

        MediaEntity me = HibernateUtils.getEntity(MediaEntity.class,Common.parseInt(id));
        try {
            if (me != null) {
                if(!me.isInternal()){
                    //find references to this media in all the entities
                    setReferencesToNull(me);
                    HibernateUtils.delete(me);
                    HibernateUtils.commit();
                }
                else {
                    returnValue.setError(I18n.getString("image.selection.error.delete.internal"));
                    logger.error(I18n.getString("image.selection.error.delete.internal"));
                }
            }
            else {
                returnValue.setError(I18n.getString("image.selection.error.delete.id"));
                logger.error(I18n.getString("image.selection.error.delete.id"));
            }
        }catch (Exception e){
            returnValue.setError(I18n.getString("image.selection.error.delete.generic"));
            logger.error(I18n.getString("image.selection.error.delete.generic"));
        }

        return returnValue;
    }

    /**
     * Internal method to search all the required entities and look for a reference to the given media id.
     * @param me Media entity to search for
     * @return true if Everything goes well
     */
    private boolean setReferencesToNull(MediaEntity me) {
        boolean res = false;

        if(me!=null){
            //Call a search for every required entity
//            searchEntityForMedia(RoomTypeEntity.class.getSimpleName(),me.getId());
            //Todo - As we integrate the image browser with more admin screens, add more calls here.
            res = true;
        }else{
            logger.info("Media not available");
        }

        return res;
    }

    /**
     * Searches an individual entity and sets all the references to null.
     * each type of entity will have it's search code.
     * @param entityName Name of the entity to search on
     * @param mediaId Media Id to search
     */
    private void searchEntityForMedia(String entityName, Integer mediaId) {
        switch (entityName){
            case "RoomTypeEntity":
//                List<RoomTypeEntity> entities = HibernateUtils.selectEntities(String.format("from %s where icon=%s", entityName,mediaId));
//                for (RoomTypeEntity curr : entities){
//                    curr.setIcon(null);
//                }
                break;
        }
    }

}
