package learnGatling;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
public class VideoGatlingApi extends Simulation {
    //config http:
    private HttpProtocolBuilder httpProcol = http
            .baseUrl("https://videogamedb.uk/api")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");


    //Define runtime parameter
    private static final int USER_COUNT = Integer.parseInt(System.getProperty(("USERS"),"5"));
    private static final int RAMUP_DURATION = Integer.parseInt(System.getProperty(("RAMUP"),"10"));
    //Feeder for test data
    private static FeederBuilder.FileBased<Object> JsonFeeder = jsonFile("data/jsonFile.json").queue();


    //Before block
    @Override

    public void before(){
        System.out.printf("Runing test with %d users%n",USER_COUNT);
        System.out.printf("Ramup using over %d seconds%n",RAMUP_DURATION);
    }

    //HTTP call
    private static ChainBuilder getAllGame =
            exec(http("Get all game")
                    .get("/videogame"));

    //Authenticate
    private static ChainBuilder authenticate =
            exec(http("Authenticate")
                    .post("/authenticate")
                    .body(StringBody("{\n" +
                            "  \"password\": \"admin\",\n" +
                            "  \"username\": \"admin\"\n" +
                            "}"))
                    .check(jmesPath("token").saveAs("jwtToken")));

    //Create game
    private static ChainBuilder createGame =
            feed(JsonFeeder)
                    .exec(http("Create Game - #{name}")
                    .post("/videogame")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .body(ElFileBody("bodies/newGameTemplate.json")).asJson());

    //Get Video Game
    private static ChainBuilder getVideoGame =
            exec(http("Get video Game - #{name}")
                    .get("/videogame/#{id}")
                    .check(jmesPath("name").isEL("#{name}")));

    //Delete video game
     private static ChainBuilder deleteVideoGame =
            exec(http("Delete game video - #{name}")
                    .delete("/videogame/#{id}")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .check(bodyString().is("Video game deleted")));

    //scenario define

    private ScenarioBuilder scn = scenario("video game stress test")
            .exec(getAllGame)
            .pause(2)
            .exec(authenticate)
            .pause(2)
            .exec(createGame)
            .pause(2)
            .exec(getVideoGame)
            .pause(2)
            .exec(deleteVideoGame)
            .pause(2);


    //load simulator
    {
        setUp(
                scn.injectOpen(
                        nothingFor(5),
                        rampUsers(USER_COUNT).during(RAMUP_DURATION)
                )
        ).protocols(httpProcol);
    }
}
