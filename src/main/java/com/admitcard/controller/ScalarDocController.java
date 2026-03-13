package com.admitcard.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScalarDocController {

    @GetMapping(value = "/api/docs", produces = MediaType.TEXT_HTML_VALUE)
    public String getApiDocs(@RequestParam(value = "theme", defaultValue = "purple") String theme) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "  <head>\n" +
                "    <title>Scalar API Reference</title>\n" +
                "    <meta charset=\"utf-8\" />\n" +
                "    <meta\n" +
                "      name=\"viewport\"\n" +
                "      content=\"width=device-width, initial-scale=1\" />\n" +
                "    <style>\n" +
                "      body {\n" +
                "        margin: 0;\n" +
                "      }\n" +
                "      .theme-selector {\n" +
                "        position: fixed;\n" +
                "        top: 10px;\n" +
                "        right: 170px;\n" +
                "        z-index: 1000;\n" +
                "        padding: 5px 10px;\n" +
                "        background: #fff;\n" +
                "        border: 1px solid #ccc;\n" +
                "        border-radius: 4px;\n" +
                "        font-family: sans-serif;\n" +
                "        box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n" +
                "      }\n" +
                "      select {\n" +
                "        padding: 2px 5px;\n" +
                "        border-radius: 3px;\n" +
                "        border: 1px solid #ddd;\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <div class=\"theme-selector\">\n" +
                "      <label for=\"theme\">Choose Template: </label>\n" +
                "      <select id=\"theme\" onchange=\"window.location.search = '?theme=' + this.value\">\n" +
                "        <option value=\"purple\" " + (theme.equals("purple") ? "selected" : "") + ">Purple</option>\n" +
                "        <option value=\"solarized\" " + (theme.equals("solarized") ? "selected" : "") + ">Solarized</option>\n" +
                "        <option value=\"bluePlanet\" " + (theme.equals("bluePlanet") ? "selected" : "") + ">Blue Planet</option>\n" +
                "        <option value=\"saturn\" " + (theme.equals("saturn") ? "selected" : "") + ">Saturn</option>\n" +
                "        <option value=\"kepler\" " + (theme.equals("kepler") ? "selected" : "") + ">Kepler</option>\n" +
                "        <option value=\"mars\" " + (theme.equals("mars") ? "selected" : "") + ">Mars</option>\n" +
                "      </select>\n" +
                "    </div>\n" +
                "    <script\n" +
                "      id=\"api-reference\"\n" +
                "      data-url=\"/v3/api-docs\"\n" +
                "      data-configuration='{\"theme\": \"" + theme + "\"}'></script>\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/@scalar/api-reference\"></script>\n" +
                "  </body>\n" +
                "</html>";
    }
}
