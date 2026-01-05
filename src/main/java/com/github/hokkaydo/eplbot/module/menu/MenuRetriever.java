package com.github.hokkaydo.eplbot.module.menu;

import java.io.File;
import java.util.Optional;

public interface MenuRetriever {

    // Retrieves the menu file if available
    Optional<File> retrieveMenu();

}
