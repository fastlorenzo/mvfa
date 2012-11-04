package be.bernardi.mvforandroid.beta.dialogs;

import be.bernardi.mvforandroid.beta.Log;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class DeleteCredentialsDialog extends DialogPreference {
	public DeleteCredentialsDialog(Context oContext, AttributeSet attrs) {
		super(oContext, attrs);
		// setDialogMessage(oContext.getString(R.string.delete_credentials_confirm));
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {
		Log.d(getClass().getSimpleName()+": Result: " + positiveResult);
		if(positiveResult) {
			getEditor().remove("token").remove("token_secret").remove("select_msisdn").commit();
		}
	}

}
