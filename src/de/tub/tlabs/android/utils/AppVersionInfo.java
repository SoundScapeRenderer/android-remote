/******************************************************************************
 * Copyright (c) 2006-2012 Quality & Usability Lab                            *
 *                         Deutsche Telekom Laboratories, TU Berlin           *
 *                         Ernst-Reuter-Platz 7, 10587 Berlin, Germany        *
 *                                                                            *
 * This file is part of the SoundScape Renderer (SSR).                        *
 *                                                                            *
 * The SSR is free software:  you can redistribute it and/or modify it  under *
 * the terms of the  GNU  General  Public  License  as published by the  Free *
 * Software Foundation, either version 3 of the License,  or (at your option) *
 * any later version.                                                         *
 *                                                                            *
 * The SSR is distributed in the hope that it will be useful, but WITHOUT ANY *
 * WARRANTY;  without even the implied warranty of MERCHANTABILITY or FITNESS *
 * FOR A PARTICULAR PURPOSE.                                                  *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * You should  have received a copy  of the GNU General Public License  along *
 * with this program.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                            *
 * The SSR is a tool  for  real-time  spatial audio reproduction  providing a *
 * variety of rendering algorithms.                                           *
 *                                                                            *
 * http://tu-berlin.de/?id=ssr                  SoundScapeRenderer@telekom.de *
 ******************************************************************************/

/* 
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
 */

package de.tub.tlabs.android.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import android.content.Context;

/**
 * Class to simplify reading AppVersionInfo.xml, an info file to determine build time/version
 * info at runtime. It is updated by AndroidAppVersionInfo.jar everytime the project is built
 * (if the .jar is added to the builders of the project).
 * 
 * @author Peter Bartz
 */
public class AppVersionInfo {
	private static final String VERSIONDATA_FILENAME = "AppVersionInfo.xml";
	private static Properties props = null;
	
	private static void readVersionInfo(Context context) throws InvalidPropertiesFormatException, IOException {
		props = new Properties();
		InputStream is = context.getAssets().open(VERSIONDATA_FILENAME);
		props.loadFromXML(is);
		is.close();
	}
	
	public static String getBuildTime(Context context) throws InvalidPropertiesFormatException, IOException {
		if (props == null) readVersionInfo(context);
		return props.getProperty("BuildTime");
	}

	public static String getBuildDate(Context context) throws InvalidPropertiesFormatException, IOException {
		if (props == null) readVersionInfo(context);
		return props.getProperty("BuildDate");
	}
	
	public static String getVersionName(Context context) throws InvalidPropertiesFormatException, IOException {
		if (props == null) readVersionInfo(context);
		return props.getProperty("VersionName");
	}
	
	public static String getVersionCode(Context context) throws InvalidPropertiesFormatException, IOException {
		if (props == null) readVersionInfo(context);
		return props.getProperty("VersionCode");
	}
}
