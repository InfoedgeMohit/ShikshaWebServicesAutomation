package common;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import javax.imageio.ImageIO;

import org.testng.*;

import com.relevantcodes.extentreports.*;

import io.restassured.path.json.JsonPath;

public class Common {

	protected static String serverPath;
	private static String mysqlDbIP;
	private static String mysqlDbUserName;
	private static String mysqlDbPassword;

	protected long apiResponseExpectedTime = 10000; // in MS
	
	public Common() {
		// load properties, apiType like - sums, location etc. [to choose particular
		// port]
		loadPropertiesFromConfig("sums");
	}

	// prepare test data
	public static String[][] prepareTestData(String[] params, String testDataPath) {
		String testDataPathFull = System.getProperty("user.dir") + testDataPath;
		int noOfParam = params.length;

		// read data from excel
		HashMap<String, HashMap<String, String>> excelData = ExcelReader.readExcel(testDataPathFull);
		int numOfTestData = excelData.size();

		// create a multi-dimensional array for test data {of size
		// [numberOfTestData][noOfParam]}
		String[][] testData = new String[numOfTestData][noOfParam];

		// keys of outer HashMap
		Set<String> keys = excelData.keySet();

		int counter = 0;
		// traverse for all keys of outer hashMap
		for (String key : keys) {
			// get inner hashMap for a particular key of outer hashMap
			HashMap<String, String> tempExcelData = excelData.get(key);
			// traverse for all keys of inner hashMap
			for (Map.Entry<String, String> entry : tempExcelData.entrySet()) {
				for (int i = 0; i < noOfParam; i++) {
					// Fetch appropriate value for key and assign to testData
					if (entry.getKey().equals(params[i]))
						testData[counter][i] = entry.getValue();
				}
			}
			counter++;
		}
		return testData;
	}

	public void loadPropertiesFromConfig(String apiType) {
		// apiType, like - sums, location etc. [to choose particular port]
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream("config.properties");
			prop.load(input);
			String serverUrl = prop.getProperty("serverUrl");
			String port = prop.getProperty("" + apiType + "Port");
			serverPath = "" + serverUrl + ":" + port + "/";
			mysqlDbIP = prop.getProperty("mysqlDbIP");
			mysqlDbUserName = prop.getProperty("mysqlDbUserName");
			mysqlDbPassword = prop.getProperty("mysqlDbPassword");
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void respnseTimeStatsCommon(ArrayList<Integer> responseTimes, String apiName) {
		Reporter.log("Response time stats for " + apiName + " api >>> ", true);

		Collections.sort(responseTimes);
		int size = responseTimes.size();

		Reporter.log("Min time : " + responseTimes.get(0) + " MS", true);
		Reporter.log(", Max time : " + responseTimes.get(size - 1) + " MS", true);

		long totalTime = 0;
		for (int item : responseTimes)
			totalTime = totalTime + item;

		Reporter.log(", Total time : " + totalTime + " MS for " + size + " inputs", true);
		Reporter.log(", Average time : " + totalTime / size + " MS", true);
	}

	public static ResultSet exceuteDbQuery(String dbQuery, String db) {
		ResultSet rs = null;
		try {
			Connection con = DriverManager.getConnection("jdbc:mysql://" + mysqlDbIP + ":3306/" + db
					+ "?zeroDateTimeBehavior=convertToNull&autoReconnect=true&"
					+ "characterEncoding=UTF-8&characterSetResults=UTF-8", mysqlDbUserName, mysqlDbPassword);
			Statement stmt = con.createStatement();
			int counter = 0;
			do {
				rs = stmt.executeQuery(dbQuery);
				counter++;
			} while (counter < 5);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rs;
		
	}

	public static int getCountOfResultSet(ResultSet rs) {
		int count = 0;
		try {
			if (rs.last()) {
				count = rs.getRow();
				rs.beforeFirst();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return count;
	}

	public static void verifyNegativeCases(String[] params, JsonPath jsonPath, String[] paramType,
			String apiResponseMsgExpected) {
		boolean flag = false;
		String fieldApiResponse = jsonPath.getString("field").trim();
		String messageApiResponse = jsonPath.getString("message").trim();
		String statusApiResponse = jsonPath.getString("status").trim();

		// in location, baseentities services, paramType field is not required
		if (paramType == null) {
			for (int i = 0; i < params.length - 1; i++) {
				if (fieldApiResponse.equalsIgnoreCase(params[i])
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)
						&& statusApiResponse.equalsIgnoreCase("error")) {
					Reporter.log(", API Response is correct [message : " + messageApiResponse + "]", true);
					
					flag = true;
					break;
				}
			}
		} else {
			String codeApiResponse = jsonPath.getString("code").trim();
			for (int i = 0; i < params.length - 1; i++) {
				if (fieldApiResponse.equalsIgnoreCase(params[i]) && codeApiResponse.equalsIgnoreCase(paramType[i])
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
					// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
					// true);
					flag = true;
					break;
				} else if (fieldApiResponse.equalsIgnoreCase(params[i])
						&& codeApiResponse.equalsIgnoreCase("{javax.validation.constraints.Min.message}")
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
					// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
					// true);
					flag = true;
					break;
				} else if (fieldApiResponse.equalsIgnoreCase(params[i].replaceAll("\\[\\]", ""))
						&& codeApiResponse.equalsIgnoreCase("{javax.validation.constraints.Size.message}")
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
					// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
					// true);
					flag = true;
					break;
				} else if (fieldApiResponse.equalsIgnoreCase(params[i])
						&& codeApiResponse.equalsIgnoreCase("{javax.validation.constraints.NotNull.message}")
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
					// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
					// true);
					flag = true;
					break;
				} else if (fieldApiResponse.equalsIgnoreCase(params[i])
						&& codeApiResponse.equalsIgnoreCase(
								"com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException")
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
					// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
					// true);
					flag = true;
					break;
				} else if (!flag && paramType[i].equals("String")) {
					if (fieldApiResponse.equalsIgnoreCase(params[i])
							&& codeApiResponse.equalsIgnoreCase("must have valid input of alphabetic String")
							&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
						// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
						// true);
						flag = true;
						break;
					}
				}
			}
		}

		if (!flag) {
			Reporter.log(", API Response message [" + messageApiResponse + "] is not correct", true);
			Assert.fail("API Response message [" + messageApiResponse + "] is not correct");
		}
	}

	public void verifyNegativeCases(String[] params, JsonPath jsonPath, String[] paramType,
			String apiResponseMsgExpected, ExtentReports report, ExtentTest parent, ExtentTest child) {
		boolean flag = false;
		String fieldApiResponse = jsonPath.getString("field").trim();
		String messageApiResponse = jsonPath.getString("message").trim();
		String statusApiResponse = jsonPath.getString("status").trim();
		
		// in location, baseentities services, paramType field is not required
		if (paramType == null) {
			for (int i = 0; i < params.length - 1; i++) {
				if (fieldApiResponse.equalsIgnoreCase(params[i])
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)
						&& statusApiResponse.equalsIgnoreCase("error")) {
					Reporter.log(", API Response is correct [message : " + messageApiResponse + "]", true);
					pass(child, "API Response is correct [message : " + messageApiResponse + "]");
					flag = true;
					break;
				}
			}
		} else {
			String codeApiResponse = jsonPath.getString("code").trim();
			for (int i = 0; i < params.length - 1; i++) {
				if (fieldApiResponse.equalsIgnoreCase(params[i]) && codeApiResponse.equalsIgnoreCase(paramType[i])
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
					// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
					// true);
					flag = true;
					break;
				} else if (fieldApiResponse.equalsIgnoreCase(params[i])
						&& codeApiResponse.equalsIgnoreCase("{javax.validation.constraints.Min.message}")
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
					// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
					// true);
					flag = true;
					break;
				} else if (fieldApiResponse.equalsIgnoreCase(params[i].replaceAll("\\[\\]", ""))
						&& codeApiResponse.equalsIgnoreCase("{javax.validation.constraints.Size.message}")
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
					// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
					// true);
					flag = true;
					break;
				} else if (fieldApiResponse.equalsIgnoreCase(params[i])
						&& codeApiResponse.equalsIgnoreCase("{javax.validation.constraints.NotNull.message}")
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
					// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
					// true);
					flag = true;
					break;
				} else if (fieldApiResponse.equalsIgnoreCase(params[i])
						&& codeApiResponse.equalsIgnoreCase(
								"com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException")
						&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
					// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
					// true);
					flag = true;
					break;
				} else if (!flag && paramType[i].equals("String")) {
					if (fieldApiResponse.equalsIgnoreCase(params[i])
							&& codeApiResponse.equalsIgnoreCase("must have valid input of alphabetic String")
							&& messageApiResponse.equalsIgnoreCase(apiResponseMsgExpected)) {
						// Reporter.log(", API Response is correct [message : "+messageApiResponse+"]",
						// true);
						flag = true;
						break;
					}
				}
			}
		}

		if (!flag) {
			Reporter.log(", API Response message [" + messageApiResponse + "] is not correct", true);
			fail(child, "API Response message [" + messageApiResponse + "] is not correct");
			Assert.fail("API Response message [" + messageApiResponse + "] is not correct");
		}
		closeChild(parent, child, report);
	}
	
	public static void verifyUnauthorizedRequestCase(JsonPath jsonPath) {
		String fieldApiResponse = jsonPath.getString("field").trim();
		String messageApiResponse = jsonPath.getString("message").trim();
		String statusApiResponse = jsonPath.getString("status").trim();

		if (fieldApiResponse.equalsIgnoreCase("request")
				&& messageApiResponse.equalsIgnoreCase("You are not authorized to send this request.")
				&& statusApiResponse.equalsIgnoreCase("error"))
			Reporter.log(", API Response is correct [message : " + messageApiResponse + "]", true);
		else {
			Reporter.log("API Response message [" + messageApiResponse + "] is not correct", true);
			Assert.fail("API Response message [" + messageApiResponse + "] is not correct");
		}
	}
	
	public  void verifyUnauthorizedRequestCase(JsonPath jsonPath,  ExtentReports report, ExtentTest parent, ExtentTest child) {
		String fieldApiResponse = jsonPath.getString("field").trim();
		String messageApiResponse = jsonPath.getString("message").trim();
		String statusApiResponse = jsonPath.getString("status").trim();

		if (fieldApiResponse.equalsIgnoreCase("request")
				&& messageApiResponse.equalsIgnoreCase("You are not authorized to send this request.")
				&& statusApiResponse.equalsIgnoreCase("error")){
			Reporter.log(", API Response is correct [message : " + messageApiResponse + "]", true);
			pass(child, "API Response is correct [message : " + messageApiResponse + "]");
		}
		else {
			Reporter.log("API Response message [" + messageApiResponse + "] is not correct", true);
			fail(child, "API Response message [" + messageApiResponse + "] is not correct");
			Assert.fail("API Response message [" + messageApiResponse + "] is not correct");
		}
		closeChild(parent, child, report);
	}
	
	public void filewriter(String string, String Filename){
		try {

	        File file = new File(Filename+".txt");
	        String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	        // if file doesnt exists, then create it
	        if (!file.exists()) {
	            file.createNewFile();
	        }

	        FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
	        BufferedWriter bw = new BufferedWriter(fw);
	        bw.write(timeLog + " : " + string);
	        bw.write("\n");
	        bw.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
			}
	public String displayTime(String dateStart){
//		 String dateStart = date;
		 String difference = null;
		 Date date = new Date();

		   // Custom date format
		    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String dateStop = format.format(date);
		    Date d1 = null;
		    Date d2 = null;
		    try {
		        d1 = format.parse(dateStart);
		        d2 = format.parse(dateStop);
		    } catch (Exception e) {
		        e.printStackTrace();
		    }

		    // Get msec from each, and subtract.
		    long diff = d2.getTime() - d1.getTime();
		    long diffSeconds = diff / 1000 % 60;
		    long diffMinutes = diff / (60 * 1000) % 60;
		    long diffHours = diff / (60 * 60 * 1000);
		    long diffdays = diffHours / 24;
		    long diffweeks = diffdays / 7;
		    long diffmonth = diffdays / 30;
		    long diffyear = diffweeks / 52;

		    if(diffyear!=0){
		    	if(diffyear ==1){
		    		difference ="a year ago";
		    	}
		    	else{
		    		difference = diffyear+" years ago";	
		    	}
		    }
		    else if(diffmonth!=0){
		    	if(diffmonth == 1){
		    		difference ="a month ago";	
		    	}
		    	else{
		    		difference =diffmonth+" months ago";
		    	}
		    }
		    else if(diffweeks!=0){
		    	if(diffweeks == 1){
		    		difference ="a week ago";	
		    	}
		    	else{
		    		difference = diffweeks+" weeks ago";
		    	}
		    }
		    else if(diffdays!=0){
		    	if(diffdays == 1){
		    		difference ="Yesterday";	
		    	}
		    	else{
		    		difference = diffdays+" days ago";
		    	}
		    }
		    else if(diffHours!=0){
		    	if(diffHours ==1){
		    		difference ="an hour ago";
		    	}
		    	else{
		    		difference = diffHours+" hours ago";	
		    	}
		    }
		    else if(diffMinutes!=0){
		    	if(diffMinutes>60&&diffMinutes<240){
		    		difference = "Few mins ago";
		    	}
		    	else if(diffMinutes>240&&diffMinutes<3600){
		    		difference = diffMinutes+" mins ago";
		    	}
		    }
		    else if(diffSeconds!=0){
		    	if(diffSeconds>0&&diffSeconds<=30){
		    		difference = "Few secs ago";
		    	}
		    	else if(diffSeconds>30&&diffSeconds<60){
		    		difference = diffSeconds+" secs ago";
		    	}
		    	difference = diffSeconds+" secs ago";
		    }
		    return difference;
	}
	 
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object parseJsonData(HashMap<Object, Object> tempMap, String keyName, String valueParam){
	
		HashMap<Object, Object> jsonInnerHashMap  = (HashMap) tempMap.get(keyName);
		if(keyName==null||keyName.length()==0){
			jsonInnerHashMap = tempMap;
		}
		Object jsonValue =jsonInnerHashMap.get(valueParam);
		return jsonValue;
	}

	public static boolean getshowComingUp(String endYear, String endMonth,
			String endDate, String startYear, String startMonth,
			String startDate) {
		boolean comingUp = false;
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String currDate = dateFormatter.format(date);
		String[] curDate = currDate.split("-");
		int currentYear = Integer.parseInt(curDate[0]);
		int currentMonth = Integer.parseInt(curDate[1]);
		int currentDate = Integer.parseInt(curDate[2]);
		if (!endYear.equals("0")) {
			if (currentYear < Integer.parseInt(endYear)) {
				comingUp = true;
			} else {
				if (currentMonth < Integer.parseInt(endMonth)&&currentYear<=Integer.parseInt(endYear)) {
					comingUp = true;
				} else {
					if ((endDate.equals("0")||currentDate <= Integer.parseInt(endDate))
							&& currentMonth <= Integer.parseInt(endMonth)
							&& currentYear <= Integer.parseInt(endYear)) {
						comingUp = true;
					} else {
						comingUp = false;
					}
				}
			}
		} else {
			if (currentYear < Integer.parseInt(startYear)) {
				comingUp = true;
			} else {
				if (currentMonth < Integer.parseInt(startMonth)&&currentYear<=Integer.parseInt(startYear)) {
					comingUp = true;
				} else {
					if ((currentDate <= Integer.parseInt(startDate)||startDate.equals("0"))&& currentMonth<=Integer.parseInt(endMonth)&&currentYear<=Integer.parseInt(startYear)) {
						comingUp = false;
					} else {
						comingUp = false;
					}
				}
			}
		} 
		return comingUp;
	}

	public static String getDisplayString(String endYear, String endMonth, String endDate, String startYear, String startMonth, String startDate){
		String finalDateStr = null;
		if(startDate.equals("0")){
			startDate=null;
		}
		
		if(endDate.equals("0")){
			endDate=null;
		}
		// end year is not null
        if(!endYear.equals("0")) {
            // check year
            if(startYear.equals(endYear)) {
                finalDateStr = getMonthName(startMonth) + " ";
                // check month
                if(startMonth.equals(endMonth)) {
                    if(startDate!=null) {
                        finalDateStr += startDate;
                    }
                    if(endDate!=null && Integer.parseInt(startDate)!=Integer.parseInt(endDate)) {
                        finalDateStr += " - "+endDate;
                    }
                } else {
                    if(startDate!= null) {
                        finalDateStr += startDate;
                    }
                    finalDateStr += " - "+ getMonthName(endMonth) + " ";
                    if(endDate!=null) {
                        finalDateStr += endDate;
                    }
                }
                finalDateStr += ", "+ startYear;
            } else {
                finalDateStr = getMonthName(startMonth) + " ";
                if(startDate!=null) {
                    finalDateStr += startDate;
                }
                finalDateStr += ", "+ startYear;
                finalDateStr += " - "+getMonthName(endMonth) + " ";
                if(endDate!=null) {
                    finalDateStr += endDate;
                }
                finalDateStr += ", "+ endYear;
            }
        } else {
            finalDateStr = getMonthName(startMonth) + " ";
            if(startDate!=null) {
                finalDateStr += startDate;
            }
            finalDateStr += ", "+ startYear;
        }

        return finalDateStr.replaceAll("  ", " ");
    }
	
	public static String getMonthName(String monthsId){
		String monthName = null;
		int monthId = Integer.parseInt(monthsId);
		 switch(monthId){
         case 1:
             monthName = "Jan";
             break;
         case 2:
             monthName = "Feb";
             break;
         case 3:
             monthName = "Mar";
             break;
         case 4:
             monthName = "Apr";
             break;
         case 5:
             monthName = "May";
             break;
         case 6:
             monthName = "Jun";
             break;
         case 7:
             monthName = "Jul";
             break;
         case 8:
             monthName = "Aug";
             break;
         case 9:
             monthName = "Sep";
             break;
         case 10:
             monthName = "Oct";
             break;
         case 11:
             monthName = "Nov";
             break;
         case 12:
             monthName = "Dec";
             break;
     } 
		return monthName;
	}
	
	public static int getDateDifferenceinMonths(String dateStart){
		Date date = new Date();

		   // Custom date format
		    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		    String dateStop = format.format(date);
		    Date d1 = null;
		    Date d2 = null;
		    try {
		        d1 = format.parse(dateStart);
		        d2 = format.parse(dateStop);
		    } catch (Exception e) {
		        e.printStackTrace();
		    }

		    // Get msec from each, and subtract.
		    long diff = d2.getTime() - d1.getTime();
		    long diffHours = diff / (60 * 60 * 1000);
		    long diffdays = diffHours / 24;
		    long diffmonth = diffdays / 30;	
		    return Integer.parseInt(String.valueOf(diffmonth));
	}
    
	public ExtentReports createExtinctReport(String moduleName) {
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        //Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String reportPath = "Report//" + moduleName
                                        //+ sdf.format(timestamp)
                                        + ".html";
        ExtentReports extent = new ExtentReports(reportPath, true);
        return extent;
}

public void closeChild(ExtentTest parent, ExtentTest child, ExtentReports report) {
        parent.appendChild(child);
        closeExtinctReport(parent, report);

}

public  void closeExtinctReport(ExtentTest parent, ExtentReports report) {
        report.endTest(parent);
        report.flush();
}

public ExtentTest createParent(ExtentReports extent, String title,
                        String description) {
        ExtentTest parent = extent.startTest(title, description);
        return parent;
}

public ExtentTest createChild(ExtentReports extent, String title,
                        String description) {
        ExtentTest child = extent.startTest(title, description);
        return child;
}

public void pass(ExtentTest child, String description) {
        child.log(LogStatus.PASS, description);
}

public void fail(ExtentTest child, String description) {
        String path = "";
        try {
                        path = "Report//";
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        Robot robot = new Robot();
                        String format = "jpg";
                        String fileName = path + "\\SS_"
                                                        + sdf.format(timestamp) + "." + format;
                        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit()
                                                        .getScreenSize());
                        BufferedImage screenFullImage = robot
                                                        .createScreenCapture(screenRect);
                        ImageIO.write(screenFullImage, format, new File(fileName));
                        child.log(LogStatus.FAIL,
                                                        description + child.addScreenCapture(fileName));

        } catch (Exception e) {
                        child.log(LogStatus.FAIL, description);
        }

}

public void info(ExtentTest child, String description) {
        child.log(LogStatus.INFO, description);
}

public void error(ExtentTest child, String description) {
        child.log(LogStatus.ERROR, description);
}

public void warning(ExtentTest child, String description) {
        child.log(LogStatus.WARNING, description);
}

public void skip(ExtentTest child, String description) {
        child.log(LogStatus.SKIP, description);
}

}


