package org.jspringbot.keyword.selenium;

import org.openqa.selenium.WebDriver;

public interface DriverCallable<T> {

    T call(WebDriver driver);
}
