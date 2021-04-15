package com.bx.carDVR.ui;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;

import com.bx.carDVR.R;


public class FormatConfirmDialog {

	private static FormatConfirmDialog mFormatConfirmDialog;
	private AlertDialog dialog = null;
	
	private FormatConfirmDialog(){
		
	}
	
	public static FormatConfirmDialog getInstance(){
		if(null == mFormatConfirmDialog){
			mFormatConfirmDialog = new FormatConfirmDialog();
		}
		return mFormatConfirmDialog;
	}

	public void showConfirmDialog(final Context mContext) {
		if (null == dialog) {
			dialog = new AlertDialog.Builder(mContext).create();
			dialog.setTitle(R.string.dialog_title_format);
			dialog.setMessage(mContext.getText(R.string.dialog_message_format_sd_card));
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(true);
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getText(R.string.ok),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(null != listener){
								listener.onFormatConfirmed();
							}
							hideConfirmDialog();
						}
					});
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getText(R.string.cancel),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(null != listener){
								listener.onFormatCanceled();
							}
							hideConfirmDialog();
						}
					});
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				
				@Override
				public void onDismiss(DialogInterface d) {
					dialog = null;
				}
			});
			dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
			dialog.show();
		} else {
			if (!dialog.isShowing()) {
				dialog.show();
			}
		}
	}

	public void hideConfirmDialog() {
		if (null != dialog && dialog.isShowing()) {
			dialog.dismiss();
			dialog = null;
		}
	}
	
	private IFormatConfirmCallBack listener = null;
	public void setFormatConfirmListener(IFormatConfirmCallBack listener){
		this.listener = listener;
	}
	
	public static interface IFormatConfirmCallBack{
		void onFormatConfirmed();
		void onFormatCanceled();
	}
}
