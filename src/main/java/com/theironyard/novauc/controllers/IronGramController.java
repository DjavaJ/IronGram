package com.theironyard.novauc.controllers;

import com.theironyard.novauc.entities.Photo;
import com.theironyard.novauc.entities.User;
import com.theironyard.novauc.services.PhotoRepository;
import com.theironyard.novauc.services.UserRepository;
import com.theironyard.novauc.utilities.PasswordStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dangelojoyce on 3/21/17.
 */
@RestController
public class IronGramController {
    @Autowired
    UserRepository users;

    @Autowired
    PhotoRepository photos;

    org.h2.tools.Server dbui = null;

    @PostConstruct
    public void init() throws SQLException {
        dbui = org.h2.tools.Server.createWebServer().start();
    }

    @PreDestroy
    public void destory() {
        dbui.stop();
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public User login(String username, String password, HttpSession session, HttpServletResponse response) throws Exception {
        User user = users.findFirstByName(username);
        if (user == null) {
            user = new User(username, PasswordStorage.createHash(password));
            users.save(user);
        } else if (!PasswordStorage.verifyPassword(password, user.getPassword())) {
            throw new Exception("Wrong password");
        }
        session.setAttribute("username", username);
        response.sendRedirect("/");
        return user;
    }

    @RequestMapping(path = "/user", method = RequestMethod.GET)
    public User getUser(HttpSession session) {
        String username = (String) session.getAttribute("username");
        return users.findFirstByName(username);
    }

    @RequestMapping("/upload")
    public Photo upload(
            HttpSession session,
            HttpServletResponse response,
            String receiver,
            MultipartFile photo,
            long time,
            boolean setting
    ) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in.");
        }

        User senderUser = users.findFirstByName(username);
        User receiverUser = users.findFirstByName(receiver);

        if (receiverUser == null) {
            throw new Exception("Receiver name doesn't exist.");
        }

        if (!photo.getContentType().startsWith("image")) {
            throw new Exception("Only images are allowed.");
        }

        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), new File("public"));
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());;



        Photo p = new Photo();
        p.setSender(senderUser);
        p.setRecipient(receiverUser);
        p.setFilename(photoFile.getName());
        p.setSetting(setting);
        p.setTime(time);
        photos.save(p);

        response.sendRedirect("/");

        return p;

    }


    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in.");
        }      User user = users.findFirstByName(username);
        timedDelete(photos.findByRecipient(user));
        return photos.findByRecipient(user);
    }

    public void timedDelete(List<Photo> allPhotos){
        for (Photo photo : allPhotos){
            TimerTask times = new TimerTask() {
                @Override
                public void run() {
                    deletePhoto(photo);
                }

            };
            Timer timer = new Timer();
            //times.scheduledExecutionTime(deletePhoto(allP);
            timer.schedule(times,photo.getTime() * 1000);
        }
    }

    public void deletePhoto(Photo photo) {
            photos.delete(photo); //delete from database
            File f = new File("public/" + photo.getFilename()); //delete from local storage
            f.delete();
    }
//    @RequestMapping(path ="/setTime", method = RequestMethod.POST)
//    public boolean changeTime(){
//
//    }


    @RequestMapping("/public-photos/{name}")
    public List pPhotos(@PathVariable ("name") String name){
        return photos.findAllBySenderAndSetting(users.findFirstByName(name), true);
    }


    @RequestMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        session.invalidate();
        response.sendRedirect("/");

    }


}
