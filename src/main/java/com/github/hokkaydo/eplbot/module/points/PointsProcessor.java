package com.github.hokkaydo.eplbot.module.points;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.points.model.Points;
import com.github.hokkaydo.eplbot.module.points.repository.PointsRepositorySQLite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.sql.DataSource;
import java.util.List;

public class PointsProcessor extends ListenerAdapter {



        private long guildId;

        //Create an empty list for relevant roles
        private List<String> roles = List.of("Shrendrickx love, Shrendrickx life","geruitiste","Rockiste","StoffelKing");
        private PointsRepositorySQLite pointsRepo;

        PointsProcessor (long guildId) {
            super();
            Main.getJDA().addEventListener(this);
            DataSource datasource = DatabaseManager.getDataSource();
            this.guildId = guildId;
            this.pointsRepo = new PointsRepositorySQLite(datasource);
            this.roles = roles;

        }
        public int getPoints(String username) {
            return this.pointsRepo.get(username);
        }

        public void addPoints(String username, int points) {
            int currentPoints = getPoints(username);
            this.pointsRepo.update(username, currentPoints + points);
        }

        public void removePoints(String username, int points) {
            int currentPoints = getPoints(username);
            this.pointsRepo.update(username, currentPoints - points);
        }

        public void setPoints(String username, int points) {
            this.pointsRepo.update(username, points);
        }

        public void resetPoints(String username) {
            this.pointsRepo.update(username, 0);
        }

        public void resetAllPoints() {
            this.pointsRepo.resetAll();

        }

        public int getPointsOfRole (String role) {
            return this.pointsRepo.getPointsOfRole(role);


        }
        public boolean hasClaimedDaily(String username,int day, int month) {
            return this.pointsRepo.dailyStatus(username,day, month);
        }

    public boolean daily(String username, int currentDay, int currentMonth) {
        //Get day and month
        Points userPoints = pointsRepo.getUser(username);
        int day = userPoints.day();
        int month = userPoints.month();
        if (day == currentDay && month == currentMonth) {
            System.out.println("Already claimed");
            return false;
        }
        addPoints(username, 25);
        //Update day and month
        this.pointsRepo.updateDate(username, currentDay, currentMonth);
        return true;
    }





    public void activateAuthor(Member author) {
            if (pointsRepo.checkPresence(author)) {
                return;
            }
        //Get author roles
        List<String> authorRoles = author.getRoles().stream().map(role -> role.getName()).toList();
        //Check if author has a role in the list of relevant roles
        if (authorRoles.stream().anyMatch(role -> roles.contains(role))) {
            //If yes, add the author to the list of active authors
            String autRole = authorRoles.stream().filter(role -> roles.contains(role)).toList().get(0);
            this.pointsRepo.create(new Points(author.getUser().getName(), 0, autRole, 0, 0));
        }
        else {
            //If not, add the author to the list of active authors with the default role
            this.pointsRepo.create(new Points(author.getUser().getName(), 0, "membre", 0, 0));
        }

    }


}
