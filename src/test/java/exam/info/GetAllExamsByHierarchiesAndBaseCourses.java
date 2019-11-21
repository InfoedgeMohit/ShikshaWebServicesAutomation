package exam.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.testng.*;
import org.testng.annotations.*;

import common.Common;

public class GetAllExamsByHierarchiesAndBaseCourses extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int basecourseId;
	String path;
	Properties prop = new Properties();
	FileInputStream file;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result = false;

	@BeforeClass
	public void doItBeforeTest() throws IOException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("exam");
		// api path
		apiPath = "exam/api/v1/info/getAllExamsByHierarchiesAndBaseCourses";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "exam", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//exam//info//GetAllExambyCourseandhierarchy.xlsx";
		path = new java.io.File(".").getCanonicalPath();
		file = new FileInputStream(
				path
						+ "\\src\\test\\resources\\exam\\info\\ExamExpectedResult.properties");
						PrintWriter pw = new PrintWriter("Logs//GetAllExamByHierarchiesAndBaseCourse.txt");
						pw.close();

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same
		// no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(priority = 1, dataProvider = "testData")
	public void verifyGetAllExamApi(String ExamIds,
			String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit api
		if (ExamIds.equals("ignoreHeader")) {
			return;
			// apiResponse =
			// RestAssured.given().when().get(api).then().extract()
			// .response();
		} else if (ExamIds.equals("ignoreInput"))
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().get(api)
					.then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath);
		} else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			verifyUnauthorizedRequestCase(jsonPath);
		} else {
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse
				.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);

		if (apiResponseActualTime > apiResponseExpectedTime) {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS", true);
		}
	}

	public void verifyPostiveCases(JsonPath jsonPath) {
		try {
			// verify status is success
			result = false;
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				Reporter.log(", Correct status [success]", true);
			} else {
				Reporter.log(", InCorrect status [" + statusResponse + "]",
						true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {
				Reporter.log(", Correct message [null]", true);
			} else {
				Reporter.log(", InCorrect message [" + messageResponse + "]",
						true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}

			try  {
				if(!jsonPath.getMap("data.course").isEmpty()){
				// fetch all substream details from db
				ResultSet rs = exceuteDbQuery(
						"select distinct  epm.id as ExamId, bc.base_course_id from exampage_main as epm join exampage_master as epmas on epm.id = epmas.exam_id join examAttributeMapping as eam on epmas.groupId = eam.groupId join exampage_groups as epg on epg.groupId = eam.groupId join base_courses as bc on eam.entityId = bc.base_course_id where bc.is_popular = 1 and eam.entityType ='course' and eam.status = 'live' and epmas.status ='live' and epm.status = 'live' order by epm.id asc;",
						"shiksha");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);
				//
				if (rowcount > 0) {
					while (rs.next()) {
						int examId = rs.getInt("ExamId");
						int courseId = rs.getInt("base_course_id");
						int groupId = 0;
						ResultSet rsverifyGroup = exceuteDbQuery(
								"select groupId from examAttributeMapping where examId = "
										+ examId
										+ " and entityId = "
										+ courseId
										+ " and entityType = 'course' and status = 'live';",
								"shiksha");
						int rowcountVerifyGroup = getCountOfResultSet(rsverifyGroup);
						if (rowcountVerifyGroup > 0) {
							while (rsverifyGroup.next()) {
								int checkgroupId = rsverifyGroup.getInt("groupId");
								if (rowcountVerifyGroup > 1) {
									ResultSet rsfindGroup = exceuteDbQuery(
											"select * from exampage_groups where examId ="
													+ examId
													+ " and groupId =" + checkgroupId
													+ " and status ='live';",
											"shiksha");
									while (rsfindGroup.next()) {
										int isPrimary = rsfindGroup.getInt("isPrimary");
										if(isPrimary !=0){
											groupId = rsfindGroup.getInt("groupId");
										}
									}
								} else {
									groupId = rsverifyGroup.getInt("groupId");
								}
							}
						}
						if(groupId ==0){
							ResultSet rsGetGroup = exceuteDbQuery(
									"select groupId from examAttributeMapping where examId = "
											+ examId
											+ " and entityId = "
											+ courseId
											+ " and entityType = 'primaryHierarchy' and status = 'live' order by groupId desc limit 1;",
									"shiksha");
							if(getCountOfResultSet(rsGetGroup)>0){
								while(rsGetGroup.next()){
									groupId = rsGetGroup.getInt("groupId");
								}
							}
						}
						ResultSet rsExamDetails = exceuteDbQuery(
								"select * from exampage_main where id ="
										+ examId + " and status = 'live';",
								"shiksha");

						// Verify Exam name of Popular Course Exam
						if (getCountOfResultSet(rsExamDetails) > 0) {
							while (rsExamDetails.next()) {
								String DBexamName = rsExamDetails
										.getString("name");
								String DBexamFullName = rsExamDetails
										.getString("fullName");
								String DBexamUrl;
								DBexamUrl = rsExamDetails.getString("url");
								if (DBexamUrl == null) {
									DBexamUrl = "";
								}
								ResultSet rsExamYear = exceuteDbQuery(
										"select entityId from examAttributeMapping where examId = "
												+ examId
												+ " and groupId = "
												+ groupId
												+ " and entityType = 'year' and status = 'live';",
										"shiksha");
								int rowCountExamyear = getCountOfResultSet(rsExamYear);
								String DBexamYear = null;

								if (rowCountExamyear > 0) {
									while (rsExamYear.next()) {
										DBexamYear = String.valueOf(rsExamYear
												.getInt("entityId"));
									}
								} else {
									DBexamYear = "";
								}
								String DBexamConductBy = "";

								ResultSet rsIsPrimary = exceuteDbQuery(
										"select * from exampage_groups where examId ="
												+ examId + " and groupId ="
												+ groupId
												+ " and status ='live';",
										"shiksha");
								int isPrimary;
								if (getCountOfResultSet(rsIsPrimary) > 0) {
									while (rsIsPrimary.next()) {
										isPrimary = rsIsPrimary
												.getInt("isPrimary");
										if (isPrimary == 0) {
											DBexamUrl = DBexamUrl + "?course="
													+ groupId;
										}
									}
								}
								
							
								String Jsonpopularcourseexamname = jsonPath
										.getString("data.course." + courseId
												+ "." + examId + ".examName");
								if(Jsonpopularcourseexamname.contains(" & ")){
									Jsonpopularcourseexamname = Jsonpopularcourseexamname.replace(" & ", " and ");
								}
								if (Jsonpopularcourseexamname
										.equalsIgnoreCase(DBexamName)) {
									Reporter.log(
											"Exam name is appear Correct for: "
													+ Jsonpopularcourseexamname,
											true);
									Assert.assertTrue(true,
											"Exam name is appear correct");
								} else {
									Reporter.log(
											"Exam name is not appear Correct for: "
													+ Jsonpopularcourseexamname
													+ " expected Result: "
													+ DBexamName, true);
									filewriter(
											"Exam name is not appear Correct for: "
													+ Jsonpopularcourseexamname
													+ " expected Result: "
													+ DBexamName
													+ " for Base Course "
													+ courseId + " ExamId: "
													+ examId + " groupId: "
													+ groupId,
											"Logs//GetAllExamByHierarchiesAndBaseCourse");
									result = true;
								}

								// Verify Exam Full name
								String Jsonpopularcourseexamfullname = jsonPath
										.getString("data.course." + courseId
												+ "." + examId
												+ ".examFullName");
								if (Jsonpopularcourseexamfullname == null) {
									Jsonpopularcourseexamfullname = "";
								}
								if (DBexamFullName == null) {
									DBexamFullName = "";
								}
								if (Jsonpopularcourseexamfullname
										.equalsIgnoreCase(DBexamFullName)) {
									Reporter.log(
											"Exam full name is appear Correct for: "
													+ Jsonpopularcourseexamname,
											true);
									Assert.assertTrue(true,
											"Exam full name is appear correct");
								} else {
									Reporter.log(
											"Exam full name is not appear Correct for: "
													+ DBexamFullName
													+ " Actual Result "
													+ Jsonpopularcourseexamname
													+ " expected Result "
													+ DBexamFullName, true);
									filewriter(
											"Exam full name is not appear Correct for: "
													+ DBexamFullName
													+ " Actual Result "
													+ Jsonpopularcourseexamname
													+ " expected Result "
													+ DBexamFullName
													+ " for Base Course "
													+ courseId + " examid: "
													+ examId + " groupId: "
													+ groupId,
											"Logs//GetAllExamByHierarchiesAndBaseCourse");
									result = true;
								}

								// Verify exam year
								String Jsonpopularcourseexamyear = jsonPath
										.getString("data.course." + courseId
												+ "." + examId + ".examYear");
								if (Jsonpopularcourseexamyear == null) {
									Jsonpopularcourseexamyear = "";
								}
								if (Jsonpopularcourseexamyear
										.equalsIgnoreCase(DBexamYear)) {
									Reporter.log(
											"Exam Year is appear Correct for: "
													+ Jsonpopularcourseexamname,
											true);
									Assert.assertTrue(true,
											"Exam Year is appear correct");
								} else {
									Reporter.log(
											"Exam year is not appear Correct for: "
													+ Jsonpopularcourseexamname
													+ " Actual Result "
													+ Jsonpopularcourseexamyear
													+ " expected Result "
													+ DBexamYear
													+ " for Base Course "
													+ courseId
													+ " for Group Id "
													+ groupId, true);
									filewriter(
											"Exam year is not appear Correct for: "
													+ Jsonpopularcourseexamname
													+ " Actual Result "
													+ Jsonpopularcourseexamyear
													+ " expected Result "
													+ DBexamYear
													+ " for Base Course "
													+ courseId + " examid: "
													+ examId + " groupId: "
													+ groupId,
											"Logs//GetAllExamByHierarchiesAndBaseCourse");
									result = true;
								}

								// verify conducted by
								String Jsonpopularcourseexamconductby = jsonPath
										.getString("data.course." + courseId
												+ "." + examId + ".conductedBy");
								if (Jsonpopularcourseexamconductby == null) {
									Jsonpopularcourseexamconductby = "";
								}
								if (Jsonpopularcourseexamconductby
										.equalsIgnoreCase(DBexamConductBy)) {
									Reporter.log(
											"Exam ConductedBy is appear Correct for: "
													+ Jsonpopularcourseexamname,
											true);
									Assert.assertTrue(true,
											"Exam ConductedBy is appear correct");
								} else {
									Reporter.log(
											"Exam ConductedBy is not appear Correct for: "
													+ Jsonpopularcourseexamname
													+ " Actual Result "
													+ Jsonpopularcourseexamname
													+ " expected Result "
													+ DBexamConductBy, true);
									filewriter(
											"Exam ConductedBy is not appear Correct for: "
													+ Jsonpopularcourseexamname
													+ " Actual Result "
													+ Jsonpopularcourseexamname
													+ " expected Result "
													+ DBexamConductBy
													+ " for Base Course "
													+ courseId + " examid: "
													+ examId + " groupId: "
													+ groupId,
											"Logs//GetAllExamByHierarchiesAndBaseCourse");
									result = true;
								}
								// Verify url
								String Jsonpopularcourseurl;

								try {
									Jsonpopularcourseurl = jsonPath
											.getString("data.course."
													+ courseId + "." + examId
													+ ".url");
								} catch (NullPointerException e) {
									Jsonpopularcourseurl = "";
								}
								if(DBexamUrl.contains("and")){
									DBexamUrl = DBexamUrl.replace("and-", "");
								}
								Jsonpopularcourseurl = Jsonpopularcourseurl
										.substring(Jsonpopularcourseurl
												.indexOf(".com") + 4);
								if (Jsonpopularcourseurl
										.equalsIgnoreCase(DBexamUrl)) {
									Reporter.log(
											"Exam Url is appear Correct for: "
													+ Jsonpopularcourseexamname,
											true);
									Assert.assertTrue(true,
											"Exam Url is appear correct");
								} else {
									Reporter.log(
											"Exam Url is not appear Correct for: "
													+ Jsonpopularcourseexamname
													+ " Actual Result "
													+ Jsonpopularcourseurl
													+ " expected Result "
													+ DBexamUrl, true);
									filewriter(
											"Exam Url is not appear Correct for: "
													+ Jsonpopularcourseexamname
													+ " Actual Result "
													+ Jsonpopularcourseurl
													+ " expected Result "
													+ DBexamUrl
													+ " for Base Course "
													+ courseId + " examid: "
													+ examId + " groupId: "
													+ groupId,
											"Logs//GetAllExamByHierarchiesAndBaseCourse");
//									result = true;
								}
							}
						}
					}
				}
				Assert.assertTrue(!result,"Popular Course Exam List Result is not appear correct please check logs");
				Reporter.log("Popular Course Map run Successfully", true);
			} else {
				Reporter.log(
						"Exam for popular Courses is empty"
								+ jsonPath.getList("data.course"), true);
			}
			}
			catch(NullPointerException e){
				Reporter.log(
						"Exam for popular Courses is empty"
								+ jsonPath.getList("data.course"), true);
			}
			try{
				if (!jsonPath.getMap("data.hierarchy").isEmpty()) {
			
				ResultSet rs = exceuteDbQuery(
						"select distinct  epm.id as ExamId, bh.hierarchy_id, bh.stream_id, epm.name,epm.fullName, epm.conductedBy, epm.url from exampage_main as epm join exampage_master as epmas on epm.id = epmas.exam_id join examAttributeMapping as eam on epmas.groupId = eam.groupId join exampage_groups as epg on epg.groupId = eam.groupId join base_hierarchies as bh on eam.entityId = bh.hierarchy_id where eam.entityType ='primaryHierarchy' and eam.status = 'live' and epmas.status ='live' and epm.status = 'live' and bh.status ='live' order by epm.id asc;",
						"shiksha");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);
				if (rowcount > 0) {
					while (rs.next()) {
						int examId = rs.getInt("ExamId");
						int hierarchyId = rs.getInt("hierarchy_id");
						int streamId = rs.getInt("stream_id");
						String dbExamName = rs.getString("name");
						String dbExamFullname = rs.getString("fullName");
						String dbExamUrl = rs.getString("url");
						String dbConductBy = "";
						int groupId = 0;
						int dbisPrimary = 0;
						int counter;
						String dbExamYear = "";
						if (streamId == hierarchyId) {
							counter = 1;
						} else {
							counter = 2;
						}
						if (dbExamUrl == null) {
							dbExamUrl = "";
						}
						ResultSet rsverifyGroup = exceuteDbQuery(
								"select groupId from examAttributeMapping where examId = "
										+ examId
										+ " and entityId = "
										+ hierarchyId
										+ " and entityType = 'primaryHierarchy' and status = 'live';",
								"shiksha");
						int rowcountVerifyGroup = getCountOfResultSet(rsverifyGroup);
						if (rowcountVerifyGroup > 0) {
							while (rsverifyGroup.next()) {
								int checkgroupId = rsverifyGroup.getInt("groupId");
								if (rowcountVerifyGroup > 1) {
									ResultSet rsfindGroup = exceuteDbQuery(
											"select * from exampage_groups where examId ="
													+ examId
													+ " and groupId =" + checkgroupId
													+ " and status ='live';",
											"shiksha");
									while (rsfindGroup.next()) {
										int isPrimary = rsfindGroup.getInt("isPrimary");
										if(isPrimary !=0){
											groupId = rsfindGroup.getInt("groupId");
										}
									}
								} else {
									groupId = rsverifyGroup.getInt("groupId");
								}
							}
						}
						if(groupId ==0){
							ResultSet rsGetGroup = exceuteDbQuery(
									"select groupId from examAttributeMapping where examId = "
											+ examId
											+ " and entityId = "
											+ hierarchyId
											+ " and entityType = 'primaryHierarchy' and status = 'live' order by groupId desc limit 1;",
									"shiksha");
							if(getCountOfResultSet(rsGetGroup)>0){
								while(rsGetGroup.next()){
									groupId = rsGetGroup.getInt("groupId");
								}
							}
						}
						ResultSet rsGetExamYear = exceuteDbQuery(
								"select * From examAttributeMapping where examId = "
										+ examId
										+ " and groupId= "
										+ groupId
										+ " and entityType = 'year' and status = 'live';",
								"shiksha");
						if (getCountOfResultSet(rsGetExamYear) > 0) {
							while (rsGetExamYear.next()) {
								dbExamYear = String.valueOf(rsGetExamYear
										.getInt("entityId"));
							}
						}

						ResultSet rsGetIsPrimary = exceuteDbQuery(
								"select * From exampage_groups where examId = "
										+ examId + " and groupId = " + groupId
										+ " and status ='live';", "shiksha");
						if (getCountOfResultSet(rsGetIsPrimary) > 0) {
							while (rsGetIsPrimary.next()) {
								dbisPrimary = rsGetIsPrimary
										.getInt("isPrimary");
							}
						}
						if (dbisPrimary == 0) {
							dbExamUrl = dbExamUrl + "?course=" + groupId;
						}
						for (; counter > 0; counter--) {
							if (counter == 1) {
								hierarchyId = streamId;
							}
							String Jsonhierarchyexamname = jsonPath
									.getString("data.hierarchy." + hierarchyId
											+ "." + examId + ".examName");

							if (Jsonhierarchyexamname
									.equalsIgnoreCase(dbExamName)) {
								Reporter.log(
										"Exam name is appear Correct for: "
												+ Jsonhierarchyexamname, true);
								Assert.assertTrue(true,
										"Exam name is appear correct");
							} else {
								Reporter.log(
										"Exam name is not appear Correct for: "
												+ Jsonhierarchyexamname
												+ " expected Result: "
												+ dbExamName, true);
								filewriter(
										"Exam name is not appear Correct for: "
												+ Jsonhierarchyexamname
												+ " expected Result: "
												+ dbExamName
												+ " for hierarchyId "
												+ hierarchyId + " examid: "
												+ examId + " groupId: "
												+ groupId,
										"Logs//GetAllExamByHierarchiesAndBaseCourse");
								result = true;
							}

							// Verify Exam Full name
							String Jsonhierarchyexamfullname = jsonPath
									.getString("data.hierarchy." + hierarchyId
											+ "." + examId + ".examFullName");
							if (Jsonhierarchyexamfullname == null) {
								Jsonhierarchyexamfullname = "";
							}
							if (dbExamFullname == null) {
								dbExamFullname = "";
							}
							if (Jsonhierarchyexamfullname
									.equalsIgnoreCase(dbExamFullname)) {
								Reporter.log(
										"Exam full name is appear Correct for: "
												+ Jsonhierarchyexamname, true);
								Assert.assertTrue(true,
										"Exam full name is appear correct");
							} else {
								Reporter.log(
										"Exam full name is not appear Correct for: "
												+ dbExamFullname
												+ " Actual Result "
												+ Jsonhierarchyexamname
												+ " expected Result "
												+ dbExamFullname, true);
								filewriter(
										"Exam full name is not appear Correct for: "
												+ dbExamFullname
												+ " Actual Result "
												+ Jsonhierarchyexamname
												+ " expected Result "
												+ dbExamFullname
												+ " for hierarchyId "
												+ hierarchyId + " examid: "
												+ examId + " groupId: "
												+ groupId,
										"Logs//GetAllExamByHierarchiesAndBaseCourse");
								result = true;
							}

							// Verify exam year
							String Jsonhierarchyexamyear = jsonPath
									.getString("data.hierarchy." + hierarchyId
											+ "." + examId + ".examYear");
							if (Jsonhierarchyexamyear == null) {
								Jsonhierarchyexamyear = "";
							}
							if (Jsonhierarchyexamyear
									.equalsIgnoreCase(dbExamYear)) {
								Reporter.log(
										"Exam Year is appear Correct for: "
												+ Jsonhierarchyexamname, true);
								Assert.assertTrue(true,
										"Exam Year is appear correct");
							} else {
								Reporter.log(
										"Exam year is not appear Correct for: "
												+ Jsonhierarchyexamname
												+ " Actual Result: "
												+ Jsonhierarchyexamyear
												+ " expected Result: "
												+ dbExamYear
												+ " for hierarchyId: "
												+ hierarchyId
												+ " for Group Id: " + groupId,
										true);
								filewriter(
										"Exam year is not appear Correct for: "
												+ Jsonhierarchyexamname
												+ " Actual Result: "
												+ Jsonhierarchyexamyear
												+ " expected Result: "
												+ dbExamYear
												+ " for hierarchyId "
												+ hierarchyId + " examid: "
												+ examId + " groupId: "
												+ groupId,
										"Logs//GetAllExamByHierarchiesAndBaseCourse");
								result = true;
							}

							// verify conducted by
							String Jsonhierarchyexamconductby = jsonPath
									.getString("data.hierarchy." + hierarchyId
											+ "." + examId + ".conductedBy");
							if (Jsonhierarchyexamconductby == null) {
								Jsonhierarchyexamconductby = "";
							}
							if (Jsonhierarchyexamconductby
									.equalsIgnoreCase(dbConductBy)) {
								Reporter.log(
										"Exam ConductedBy is appear Correct for: "
												+ Jsonhierarchyexamname, true);
								Assert.assertTrue(true,
										"Exam ConductedBy is appear correct");
							} else {
								Reporter.log(
										"Exam ConductedBy is not appear Correct for: "
												+ Jsonhierarchyexamname
												+ " Actual Result "
												+ Jsonhierarchyexamname
												+ " expected Result "
												+ dbConductBy, true);
								filewriter(
										"Exam ConductedBy is not appear Correct for: "
												+ Jsonhierarchyexamname
												+ " Actual Result "
												+ Jsonhierarchyexamname
												+ " expected Result "
												+ dbConductBy
												+ " for hierarchyId "
												+ hierarchyId + " examid: "
												+ examId + " groupId: "
												+ groupId,
										"Logs//GetAllExamByHierarchiesAndBaseCourse");
								result = true;
							}
							// Verify url
							String Jsonhierarchyurl;

							try {
								Jsonhierarchyurl = jsonPath
										.getString("data.hierarchy."
												+ hierarchyId + "." + examId
												+ ".url");
							} catch (NullPointerException e) {
								Jsonhierarchyurl = "";
							}

							Jsonhierarchyurl = Jsonhierarchyurl
									.substring(Jsonhierarchyurl.indexOf(".com") + 4);
							if (Jsonhierarchyurl.equalsIgnoreCase(dbExamUrl)) {
								Reporter.log("Exam Url is appear Correct for: "
										+ Jsonhierarchyexamname, true);
								Assert.assertTrue(true,
										"Exam Url is appear correct");
							} else {
								Reporter.log(
										"Exam Url is not appear Correct for: "
												+ Jsonhierarchyexamname
												+ " Actual Result "
												+ Jsonhierarchyurl
												+ " expected Result "
												+ dbExamUrl, true);
								filewriter(
										"Exam Url is not appear Correct for: "
												+ Jsonhierarchyexamname
												+ " Actual Result "
												+ Jsonhierarchyurl
												+ " expected Result "
												+ dbExamUrl
												+ " for hierarchyId "
												+ hierarchyId + " examid: "
												+ examId + " groupId: "
												+ groupId,
										"Logs//GetAllExamByHierarchiesAndBaseCourse");
//								result = true;
							}
						}
					}
				}
				Assert.assertTrue(!result, "Hierachies Exam list Failed Please Check Logs");
				Reporter.log("Hierachies Map Run Successfully", true);
			} else {
				Reporter.log(
						"Exam for hierarchy Courses is empty"
								+ jsonPath.getList("data.hierarchy"), true);
			}
			}
			catch(NullPointerException e){
				Reporter.log(
						"Exam for hierarchy Courses is empty"
								+ jsonPath.getList("data.hierarchy"), true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}

	}

	@Test(priority = 2)
	public static void respnseTimeStats_GetAllExamsByHierarchiesAndBaseCourses() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}
