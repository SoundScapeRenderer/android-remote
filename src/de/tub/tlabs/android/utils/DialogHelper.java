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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;


/**
 * Contains static methods to simiplify creation of progress dialogs
 * and of dialogs with one, two or three buttons. 
 * 
 * @author Peter Bartz
 */
public class DialogHelper {
	public static AlertDialog showOneButtonDialog(Context context, String title, String message, String okButtonText) {
		return showOneButtonDialog(context, title, message, okButtonText, null);
	}

	public static AlertDialog showOneButtonDialog(Context context, String title, String message, String okButtonText, DialogInterface.OnClickListener okButtonClickListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title).setMessage(message).setCancelable(false).setPositiveButton(okButtonText, okButtonClickListener);
		return builder.show();
	}

	public static AlertDialog showTwoButtonDialog(Context context, String title, String message, String yesButtonText, DialogInterface.OnClickListener yesButtonClickListener, 
			String noButtonText, DialogInterface.OnClickListener noButtonClickListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title).setMessage(message).setCancelable(false).setPositiveButton(yesButtonText, yesButtonClickListener).setNegativeButton(noButtonText, noButtonClickListener);
		return builder.show();
	}

	public static AlertDialog showThreeButtonDialog(Context context, String title, String message, String yesButtonText, DialogInterface.OnClickListener yesButtonClickListener, 
			String noButtonText, DialogInterface.OnClickListener noButtonClickListener, String cancelButtonText, DialogInterface.OnClickListener cancelButtonClickListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title).setMessage(message).setCancelable(false).setPositiveButton(yesButtonText, yesButtonClickListener).setNegativeButton(noButtonText, noButtonClickListener).
			setNeutralButton(cancelButtonText, cancelButtonClickListener);
		return builder.show();
	}

	public static ProgressDialog showProgressBarDialog(Context context, String title, String message, int maxProgress) {
		ProgressDialog progressDialog = new ProgressDialog(context);
		progressDialog.setTitle(title);
		progressDialog.setMessage(message);
		progressDialog.setCancelable(false);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setMax(maxProgress);
		progressDialog.show();
		return progressDialog;
	}
	
	public static ProgressDialog showProgressDialog(Context context, String title, String message) {
		ProgressDialog progressDialog = new ProgressDialog(context);
		progressDialog.setTitle(title);
		progressDialog.setMessage(message);
		progressDialog.setCancelable(false);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.show();
		return progressDialog;
	}
}
