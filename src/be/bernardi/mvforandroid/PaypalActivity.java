package be.bernardi.mvforandroid;

import java.math.BigDecimal;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.paypal.android.MEP.CheckoutButton;
import com.paypal.android.MEP.PayPal;
import com.paypal.android.MEP.PayPalPayment;

import be.bernardi.mvforandroid.data.ResultDelegate;

public class PaypalActivity extends Activity implements OnClickListener {
	// The PayPal server to be used - can also be ENV_NONE and ENV_LIVE
	private static final int	server				= PayPal.ENV_SANDBOX;
	// The ID of your application that you received from PayPal
	// private static final String appID = "APP-4T985392147246637"; // LIVE
	private static final String	appID				= "APP-80W284485P519543T";	// SANDBOX
	// This is passed in for the startActivityForResult() android function, the
	// value used is up to you
	private static final int	request				= 1;

	public static final String	build				= "10.12.09.8053";

	protected static final int	INITIALIZE_SUCCESS	= 0;
	protected static final int	INITIALIZE_FAILURE	= 1;

	protected static float		amount				= 0;

	protected ProgressDialog	dialog;

	Button						donate_btn;
	TextView					title;

	// You will need at least one CheckoutButton, this application has four for
	// examples
	CheckoutButton				launchSimplePayment;

	// These are used to display the results of the transaction
	public static String		resultTitle;
	public static String		resultInfo;
	public static String		resultExtra;

	// This handler will allow us to properly update the UI. You cannot touch
	// Views from a non-UI thread.
	Handler						hRefresh			= new Handler() {
														@Override
														public void handleMessage(Message msg) {
															switch(msg.what) {
																case INITIALIZE_SUCCESS:
																	dialog.hide();
																	break;
																case INITIALIZE_FAILURE:
																	dialog.hide();
																	showFailure();
																	break;
															}
														}
													};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.paypal);

		donate_btn = (Button) findViewById(R.id.donate_btn);
		donate_btn.setOnClickListener(this);
		dialog = ProgressDialog.show(PaypalActivity.this, "", getString(R.string.paypal_loading), true);
		// Initialize the library. We'll do it in a separate thread because it
		// requires communication with the server
		// which may take some time depending on the connection strength/speed.
		dialog.show();
		Thread libraryInitializationThread = new Thread() {
			public void run() {
				initLibrary();
				// The library is initialized so let's create our
				// CheckoutButton and update the UI.
				if(PayPal.getInstance().isLibraryInitialized()) {
					hRefresh.sendEmptyMessage(INITIALIZE_SUCCESS);
				}
				else {
					hRefresh.sendEmptyMessage(INITIALIZE_FAILURE);
				}
			}
		};
		libraryInitializationThread.start();
	}

	/**
	 * Show a failure message because initialization failed.
	 */
	public void showFailure() {
		Toast toast = Toast.makeText(this, getString(R.string.paypal_failed), 500);
		toast.show();
		finish();
	}

	/**
	 * The initLibrary function takes care of all the basic Library
	 * initialization.
	 * 
	 * @return The return will be true if the initialization was successful and
	 *         false if
	 */
	private void initLibrary() {
		PayPal pp = PayPal.getInstance();
		// If the library is already initialized, then we don't need to
		// initialize it again.
		if(pp == null) {
			pp = PayPal.initWithAppID(this, appID, server);
			pp.setLanguage("en_US"); // Sets the language for the library.
			pp.setFeesPayer(PayPal.FEEPAYER_SENDER);
			pp.setShippingEnabled(false);
			pp.setDynamicAmountCalculationEnabled(false);
		}
	}

	/**
	 * Create a PayPalPayment which is used for simple payments.
	 * 
	 * @return Returns a PayPalPayment.
	 */
	private PayPalPayment exampleSimplePayment(String amount) {
		// Create a basic PayPalPayment.
		PayPalPayment payment = new PayPalPayment();
		// Sets the currency type for this payment.
		payment.setCurrencyType("EUR");
		// Sets the recipient for the payment. This can also be a phone number.
		// payment.setRecipient("fastlorenzo@gmail.com");
		payment.setRecipient("merch_1305122465_biz@gmail.com");
		// Sets the amount of the payment, not including tax and shipping
		// amounts.
		Log.v("PAYPAL", "amount=" + amount);
		payment.setSubtotal(new BigDecimal(amount));
		// Sets the payment type. This can be PAYMENT_TYPE_GOODS,
		// PAYMENT_TYPE_SERVICE, PAYMENT_TYPE_PERSONAL, or PAYMENT_TYPE_NONE.
		payment.setPaymentType(PayPal.PAYMENT_TYPE_PERSONAL);
		// payment.setPaymentSubtype(PayPal.PAYMENT_SUBTYPE_DONATIONS);

		// Sets the PayPalPayment invoice data.
		// payment.setInvoiceData(invoice);
		// Sets the merchant name. This is the name of your Application or
		// Company.
		payment.setMerchantName("Lorenzo Bernardi");
		// Sets the description of the payment.
		payment.setDescription(getString(R.string.paypal_thanks));
		// Sets the Custom ID. This is any ID that you would like to have
		// associated with the payment.
		// payment.setCustomID("8873482296");
		// Sets the Instant Payment Notification url. This url will be hit by
		// the PayPal server upon completion of the payment.
		// payment.setIpnUrl("http://www.exampleapp.com/ipn");
		// Sets the memo. This memo will be part of the notification sent by
		// PayPal to the necessary parties.
		// payment.setMemo("Hi! I'm making a memo for a simple payment.");

		return payment;
	}

	public void onClick(View v) {

		/**
		 * For each call to checkout() and preapprove(), we pass in a
		 * ResultDelegate. If you want your application to be notified as soon
		 * as a payment is completed, then you need to create a delegate for
		 * your application. The delegate will need to implement
		 * PayPalResultDelegate and Serializable. See our ResultDelegate for
		 * more details.
		 */

		if(v == donate_btn) {
			EditText amount = (EditText) findViewById(R.id.donate_amount);
			// Use our helper function to create the simple payment.
			if((amount.getText() + "-").equals("-")) {
				Toast toast = Toast.makeText(this, getString(R.string.no_amount), 500);
				toast.show();
				return;
			}
			PayPalPayment payment = exampleSimplePayment(amount.getText().toString());
			// Use checkout to create our Intent.
			Intent checkoutIntent = PayPal.getInstance().checkout(payment, this, new ResultDelegate());
			// Use the android's startActivityForResult() and pass in our
			// Intent. This will start the library.
			startActivityForResult(checkoutIntent, request);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode != request)
			return;
	}
}