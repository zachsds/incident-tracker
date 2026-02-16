package com.sdsweather;


import com.sdsweather.navigation.Navigator;
import com.sdsweather.ui.LoginPage;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Navigator.init(stage);

        Navigator.show(new LoginPage());
    }

    public static void main(String[] args) {
        launch();
    }
}
