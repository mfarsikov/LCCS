package controller;

import model.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by igor on 14.10.16.
 */

@RestController
public class WebController {

    private SQLiteHandler2 dbHandler = new SQLiteHandler2();
    private HttpExtractor extractor = new HttpExtractor();


    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getMainPage () {
        return getTextFileForBrowser("index.html");
    }


    @RequestMapping(value = "/{file:.+}", method = RequestMethod.GET)
    public String getFile(@PathVariable("file") String filename) {
        return getTextFileForBrowser(filename);
    }


    @RequestMapping(value = "/tracked_numbers", method = RequestMethod.GET, produces = "application/json")
    public List<NumberTransferObject> showAllNumbers() {
        return dbHandler.getAllNumbers();
    }


    @RequestMapping(value = "/tracked_numbers", method = RequestMethod.POST)
    public void addNumber(@RequestBody String number) {
        dbHandler.addNumber(number);
    }


    @RequestMapping(value = "/tracked_numbers/{num_id}", method = RequestMethod.DELETE)
    public void deleteNumber(@PathVariable("num_id") int id) {
        dbHandler.deleteNumberById(id);
    }


    @RequestMapping(value = "/hearings", method = RequestMethod.GET, produces = "application/json")
    public List<CourtCase> showCurrentCases() {
        return updateCaseList();
    }


    private String getTextFileForBrowser(String filename) {
        StringBuilder sb = new StringBuilder();
        InputStream is = getClass().getResourceAsStream(("/webUI/"+filename));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line = reader.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private List<CourtCase> updateCaseList() {
        List<NumberTransferObject> numbTransObjList = dbHandler.getAllNumbers();
        List<String> allIds = new ArrayList<>();
        for (NumberTransferObject nto : numbTransObjList) {
            allIds.add(nto.getNumber());
        }
        List<CourtCase> courtCases = extractor.extractCourtCases(allIds);
        dbHandler.save(courtCases);
        return courtCases;
    }

}
