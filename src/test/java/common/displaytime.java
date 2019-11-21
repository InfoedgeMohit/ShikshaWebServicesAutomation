package common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class displaytime {
	
	@SuppressWarnings("unused")
	public static void main(String[] args){

//		 String dateStart = date;
		String endYear = null;
		String endMonth=null;
		String endDate=null;	
		String startDate="2", startYear="2019", startMonth="3";
		
		boolean comingUp = false;
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String currDate = dateFormatter.format(date);
		String[] curDate = currDate.split("-");
		int currentYear = Integer.parseInt(curDate[0]);
		int currentMonth = Integer.parseInt(curDate[1]);
		int currentDate = Integer.parseInt(curDate[2]);
		if (endYear != null) {
			if (currentYear < Integer.parseInt(endYear)) {
				comingUp = true;
			} else {
				if (currentMonth < Integer.parseInt(endMonth)&&currentYear<=Integer.parseInt(endYear)) {
					comingUp = true;
				} else {
					if ((endDate == null||currentDate < Integer.parseInt(endDate))
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
					if ((currentDate < Integer.parseInt(startDate)||startDate==null)&& currentMonth<=Integer.parseInt(endMonth)&&currentYear<=Integer.parseInt(startYear)) {
						comingUp = false;
					} else {
						comingUp = false;
					}
				}
			}
		} 
		    System.out.print("asdf: "+comingUp);
	
	}
}
