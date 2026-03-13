package com.admitcard.component;

import com.admitcard.model.Template;
import com.admitcard.repository.TemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final TemplateRepository templateRepository;

    public DataLoader(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Modern Official Template only if it doesn't exist
        if (!templateRepository.findAll().stream().anyMatch(t -> t.getName().equalsIgnoreCase("Official Admit Card"))) {
            Template defaultTemplate = Template.builder()
                    .name("Official Admit Card")
                    .description("The standard academic admit card template with student and college details.")
                    .htmlContent("<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "<head>\n" +
                            "    <style>\n" +
                            "        .page { width: 180mm; margin: 0 auto; font-family: sans-serif; border: 2px solid #333; padding: 20px; color: #1e293b; }\n" +
                            "        .header { text-align: center; border-bottom: 2px solid #333; margin-bottom: 20px; padding-bottom: 10px; }\n" +
                            "        .title { font-size: 24pt; font-weight: bold; margin: 0; color: #1e1b4b; }\n" +
                            "        .detail-row { display: flex; margin-bottom: 15px; border-bottom: 1px dashed #ccc; padding-bottom: 5px; }\n" +
                            "        .label { font-weight: bold; width: 140px; color: #475569; }\n" +
                            "        .value { flex: 1; }\n" +
                            "        .footer { margin-top: 50px; display: flex; justify-content: space-between; }\n" +
                            "        .sig { border-top: 1px solid #333; width: 200px; text-align: center; padding-top: 5px; font-weight: bold; }\n" +
                            "    </style>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "    <div class=\"page\">\n" +
                            "        <div class=\"header\">\n" +
                            "            <div class=\"title\">ACADEMIC ADMIT CARD</div>\n" +
                            "            <div style=\"font-size: 12pt; margin-top: 5px;\">Session: 2023-26</div>\n" +
                            "        </div>\n" +
                            "        \n" +
                            "        <div class=\"detail-row\">\n" +
                            "            <div class=\"label\">Candidate Name:</div>\n" +
                            "            <div class=\"value\">{{name}}</div>\n" +
                            "        </div>\n" +
                            "        <div class=\"detail-row\">\n" +
                            "            <div class=\"label\">Roll Number:</div>\n" +
                            "            <div class=\"value\">{{roll}}</div>\n" +
                            "        </div>\n" +
                            "        <div class=\"detail-row\">\n" +
                            "            <div class=\"label\">Registration:</div>\n" +
                            "            <div class=\"value\">{{registration}}</div>\n" +
                            "        </div>\n" +
                            "        <div class=\"detail-row\">\n" +
                            "            <div class=\"label\">Course:</div>\n" +
                            "            <div class=\"value\">{{course}}</div>\n" +
                            "        </div>\n" +
                            "        <div class=\"detail-row\">\n" +
                            "            <div class=\"label\">Subject:</div>\n" +
                            "            <div class=\"value\">{{subject}}</div>\n" +
                            "        </div>\n" +
                            "\n" +
                            "        <div class=\"footer\">\n" +
                            "            <div class=\"sig\">Student Signature</div>\n" +
                            "            <div class=\"sig\">Controller of Exams</div>\n" +
                            "        </div>\n" +
                            "    </div>\n" +
                            "</body>\n" +
                            "</html>")
                    .build();
            templateRepository.save(defaultTemplate);
        }

        // 2. Seed Legacy Template only if it doesn't exist
        if (!templateRepository.findAll().stream().anyMatch(t -> t.getName().equalsIgnoreCase("Legacy Professional Template"))) {
            Template legacyTemplate = Template.builder()
                    .name("Legacy Professional Template")
                    .description("The previous reliable template with a classic table-based layout.")
                    .htmlContent("<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "\n" +
                            "<head>\n" +
                            "    <style>\n" +
                            "        * { box-sizing: border-box; }\n" +
                            "        body { margin: 0; padding: 0; font-family: \"Helvetica\", \"Arial\", sans-serif; background-color: white; }\n" +
                            "        .page-container { width: 185mm; margin: 0 auto; padding: 10mm; }\n" +
                            "        .header-box { width: 100%; text-align: center; border-bottom: 2px solid #000; margin-bottom: 20px; padding-bottom: 10px; }\n" +
                            "        .college-title { font-size: 18pt; font-weight: bold; margin: 0; }\n" +
                            "        .exam-sub-title { font-size: 11pt; margin: 5px 0; color: #444; }\n" +
                            "        .info-table { width: 100%; border-collapse: collapse; table-layout: fixed; margin-bottom: 20px; }\n" +
                            "        .info-table td { padding: 6px 2px; font-size: 10pt; vertical-align: bottom; }\n" +
                            "        .label { font-weight: bold; width: 110px; }\n" +
                            "        .field { border-bottom: 1px solid #000; }\n" +
                            "        .grid-table { width: 100%; border-collapse: collapse; table-layout: fixed; margin: 20px 0; }\n" +
                            "        .grid-table th { border: 1px solid #000; background-color: #f2f2f2; padding: 8px; font-size: 9pt; text-align: center; }\n" +
                            "        .grid-table td { border: 1px solid #000; padding: 8px; font-size: 9pt; text-align: center; word-wrap: break-word; }\n" +
                            "        .sig-box { width: 40%; text-align: center; font-weight: bold; font-size: 10pt; border-top: 1px solid #000; padding-top: 5px; }\n" +
                            "    </style>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "    <div class=\"page-container\">\n" +
                            "        <div class=\"header-box\">\n" +
                            "            <h1 class=\"college-title\">{{college_name}}</h1>\n" +
                            "            <div class=\"exam-sub-title\">{{exam_name}}</div>\n" +
                            "        </div>\n" +
                            "        <div style=\"text-align: center; font-weight: bold; font-size: 14pt; margin-bottom: 20px;\">ADMIT CARD</div>\n" +
                            "        <table class=\"info-table\">\n" +
                            "            <tr>\n" +
                            "                <td class=\"label\">Name:</td>\n" +
                            "                <td class=\"field\">{{name}}</td>\n" +
                            "                <td style=\"width:20px\"></td>\n" +
                            "                <td class=\"label\">Roll Number:</td>\n" +
                            "                <td class=\"field\">{{roll}}</td>\n" +
                            "            </tr>\n" +
                            "            <tr>\n" +
                            "                <td class=\"label\">Registration No:</td>\n" +
                            "                <td class=\"field\">{{reg}}</td>\n" +
                            "                <td style=\"width:20px\"></td>\n" +
                            "                <td class=\"label\">Course:</td>\n" +
                            "                <td class=\"field\">{{course}}</td>\n" +
                            "            </tr>\n" +
                            "        </table>\n" +
                            "        <table class=\"grid-table\">\n" +
                            "            <thead>\n" +
                            "                <tr>\n" +
                            "                    <th style=\"width: 40%;\">Subject Code & Name</th>\n" +
                            "                    <th>Date</th>\n" +
                            "                    <th>Time</th>\n" +
                            "                    <th style=\"width: 15%;\">Room</th>\n" +
                            "                </tr>\n" +
                            "            </thead>\n" +
                            "            <tbody>\n" +
                            "                <tr>\n" +
                            "                    <td>{{subject}}</td>\n" +
                            "                    <td>{{date}}</td>\n" +
                            "                    <td>{{time}}</td>\n" +
                            "                    <td>{{room}}</td>\n" +
                            "                </tr>\n" +
                            "            </tbody>\n" +
                            "        </table>\n" +
                            "        <table style=\"width: 100%; margin-top: 80px;\">\n" +
                            "            <tr>\n" +
                            "                <td class=\"sig-box\">Candidate Signature</td>\n" +
                            "                <td style=\"width: 20%;\"></td>\n" +
                            "                <td class=\"sig-box\">Controller of Exams</td>\n" +
                            "            </tr>\n" +
                            "        </table>\n" +
                            "    </div>\n" +
                            "</body>\n" +
                            "</html>")
                    .build();
            templateRepository.save(legacyTemplate);
        }
    }
}
