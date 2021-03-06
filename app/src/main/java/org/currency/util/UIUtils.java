package org.currency.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle2.cms.SignerId;
import org.currency.App;
import org.currency.activity.ActivityBase;
import org.currency.activity.FragmentContainerActivity;
import org.currency.activity.MessageActivity;
import org.currency.android.R;
import org.currency.dto.ResponseDto;
import org.currency.dto.UserDto;
import org.currency.fragment.MessageDialogFragment;
import org.currency.ui.DialogButton;
import org.currency.xades.XmlSignature;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static org.currency.util.LogUtils.LOGD;

/**
 * An assortment of UI helpers.
 *
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UIUtils {

    private static final String TAG = UIUtils.class.getSimpleName();

    public static final int EMPTY_MESSAGE = 1;
    /**
     * Regex to search for HTML escape sequences.
     * <p>
     * <p></p>Searches for any continuous string of characters starting with an ampersand and ending with a
     * semicolon. (Example: &amp;amp;)
     */
    public static final String TARGET_FORM_FACTOR_HANDSET = "handset";
    public static final String TARGET_FORM_FACTOR_TABLET = "tablet";
    private static final Pattern REGEX_HTML_ESCAPE = Pattern.compile(".*&\\S;.*");
    public static final int ANIMATION_FADE_IN_TIME = 250;
    public static final String TRACK_ICONS_TAG = "tracks";
    private static SimpleDateFormat sDayOfWeekFormat = new SimpleDateFormat("E");
    private static DateFormat sShortTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);


    public static void launchMessageActivity(ResponseDto responseDto) {
        Intent intent = new Intent(App.getInstance(), MessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.RESPONSE_KEY, responseDto);
        App.getInstance().startActivity(intent);
    }

    public static void launchMessageActivity(Integer statusCode, String message, String caption) {
        ResponseDto responseDto = new ResponseDto(statusCode);
        responseDto.setCaption(caption).setNotificationMessage(message);
        Intent intent = new Intent(App.getInstance(), MessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.RESPONSE_KEY, responseDto);
        App.getInstance().startActivity(intent);
    }

    public static void showSignersInfoDialog(Set<XmlSignature> signatures,
                             FragmentManager fragmentManager, Context context) {
        StringBuilder signersInfo = new StringBuilder(context.getString(R.string.num_signers_lbl,
                signatures.size()) + "<br/><br/>");
        for (XmlSignature signature : signatures) {
            X509Certificate certificate = signature.getSigningCertificate();
            signersInfo.append(context.getString(R.string.cert_info_formated_msg,
                    certificate.getSubjectDN().toString(),
                    certificate.getIssuerDN().toString(),
                    certificate.getSerialNumber().toString(),
                    DateUtils.getDayWeekDateStr(certificate.getNotBefore(), "HH:mm"),
                    DateUtils.getDayWeekDateStr(certificate.getNotAfter(), "HH:mm")) + "<br/>");
        }
        MessageDialogFragment.showDialog(ResponseDto.SC_OK, context.getString(
                R.string.signers_info_lbl), signersInfo.toString(), fragmentManager);
    }

    public static void showCMSSignersInfoDialog(Set<UserDto> signers, FragmentManager fragmentManager,
                                             Context context) {
        StringBuilder signersInfo = new StringBuilder(context.getString(R.string.num_signers_lbl,
                signers.size()) + "<br/><br/>");
        for(UserDto signer : signers) {
            X509Certificate certificate = signer.getX509Certificate();
            signersInfo.append(context.getString(R.string.cert_info_formated_msg,
                    certificate.getSubjectDN().toString(),
                    certificate.getIssuerDN().toString(),
                    certificate.getSerialNumber().toString(),
                    DateUtils.getDayWeekDateStr(certificate.getNotBefore(), "HH:mm"),
                    DateUtils.getDayWeekDateStr(certificate.getNotAfter(), "HH:mm")) + "<br/>");
        }
        MessageDialogFragment.showDialog(ResponseDto.SC_OK, context.getString(
                R.string.signers_info_lbl), signersInfo.toString(), fragmentManager);
    }

    public static Drawable getEmptyLogo(Context context) {
        Drawable drawable = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));
        return drawable;
    }

    public static void showTimeStampInfoDialog(TimeStampToken timeStampToken,
                                       FragmentManager fragmentManager, Context context) {
        try {
            TimeStampTokenInfo tsInfo = timeStampToken.getTimeStampInfo();
            String certificateInfo = null;
            SignerId signerId = timeStampToken.getSID();
            String dateInfoStr = DateUtils.getDayWeekDateStr(tsInfo.getGenTime(), "HH:mm");
            /*CollectionStore store = (CollectionStore) timeStampToken.getCertificates();
            Collection<X509CertificateHolder> matches = store.getMatches(signerId);
            X509CertificateHolder certificateHolder = null;
            if (matches.size() == 0) {
                LOGD(TAG, "showTimeStampInfoDialog - no cert matches found, validating with timestamp server cert");
                certificateHolder = new X509CertificateHolder(timeStampServerCert.getEncoded());
                timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                        Constants.PROVIDER).build(certificateHolder));
            } else certificateHolder = matches.iterator().next();
            LOGD(TAG + ".showTimeStampInfoDialog", "serial number: '" +
                    certificateHolder.getSerialNumber() + "'");
            X509Certificate certificate = new JcaX509CertificateConverter().
                    getCertificate(certificateHolder);*/
            certificateInfo = context.getString(R.string.timestamp_info_formated_msg, dateInfoStr,
                    tsInfo.getSerialNumber().toString(),
                    timeStampToken.getSID().getSerialNumber().toString());
            MessageDialogFragment.showDialog(ResponseDto.SC_OK, context.getString(
                    R.string.timestamp_info_lbl), certificateInfo, fragmentManager);
        } catch (Exception ex) {
            ex.printStackTrace();
            MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, context.getString(
                    R.string.error_lbl), context.getString(R.string.timestamp_error_lbl),
                    fragmentManager);
        }
    }

    public static boolean isSameDayDisplay(long time1, long time2) {
        TimeZone displayTimeZone = PrefUtils.getDisplayTimeZone();
        Calendar cal1 = Calendar.getInstance(displayTimeZone);
        Calendar cal2 = Calendar.getInstance(displayTimeZone);
        cal1.setTimeInMillis(time1);
        cal2.setTimeInMillis(time2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    //http://stackoverflow.com/questions/15055458/detect-7-inch-and-10-inch-tablet-programmatically
    public static double getDiagonalInches(Display display) {
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        //float scaleFactor = metrics.density;
        //float widthDp = widthPixels / scaleFactor;
        //float heightDp = heightPixels / scaleFactor;
        float widthDpi = metrics.xdpi;
        float heightDpi = metrics.ydpi;
        float widthInches = widthPixels / widthDpi;
        float heightInches = heightPixels / heightDpi;
        double diagonalInches = Math.sqrt((widthInches * widthInches) +
                (heightInches * heightInches));
        return diagonalInches;
    }

    /**
     * Populate the given {@link android.widget.TextView} with the requested text, formatting
     * through {@link android.text.Html#fromHtml(String)} when applicable. Also sets
     * {@link android.widget.TextView#setMovementMethod} so inline links are handled.
     */
    public static void setTextMaybeHtml(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setText("");
            return;
        }
        if ((text.contains("<") && text.contains(">")) || REGEX_HTML_ESCAPE.matcher(text).find()) {
            view.setText(Html.fromHtml(text));
            view.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            view.setText(text);
        }
    }

    /**
     * Given a snippet string with matching segments surrounded by curly
     * braces, turn those areas into bold spans, removing the curly braces.
     */
    public static Spannable buildStyledSnippet(String snippet) {
        final SpannableStringBuilder builder = new SpannableStringBuilder(snippet);
        // Walk through string, inserting bold snippet spans
        int startIndex, endIndex = -1, delta = 0;
        while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
            endIndex = snippet.indexOf('}', startIndex);
            // Remove braces from both sides
            builder.delete(startIndex - delta, startIndex - delta + 1);
            builder.delete(endIndex - delta - 1, endIndex - delta);
            // Insert bold style
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    startIndex - delta, endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //builder.setSpan(new ForegroundColorSpan(0xff111111),
            //        startIndex - delta, endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            delta += 2;
        }
        return builder;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static void changeSearchIcon(Menu menu, Context context) {
        MenuItem searchViewMenuItem = menu.findItem(R.id.search_item);
        SearchView mSearchView = (SearchView) searchViewMenuItem.getActionView();
        int searchImgId = context.getResources().getIdentifier("android:id/search_button", null, null);
        ImageView v = (ImageView) mSearchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.action_search);
    }

    public static EditText addFormField(String label, Integer type, LinearLayout mFormView, int id,
                                        Context context) {
        TextView textView = new TextView(context);
        textView.setTextSize(context.getResources().getDimension(R.dimen.claim_field_text_size));
        textView.setText(label);
        EditText fieldText = new EditText(context.getApplicationContext());
        fieldText.setLayoutParams(UIUtils.getFormItemParams(false));
        fieldText.setTextColor(Color.BLACK);
        // setting an unique id is important in order to save the state
        // (content) of this view across screen configuration changes
        fieldText.setId(id);
        fieldText.setInputType(type);
        mFormView.addView(textView);
        mFormView.addView(fieldText);
        return fieldText;
    }

    public static LinearLayout.LayoutParams getFormItemParams(boolean isLabel) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (isLabel) {
            params.bottomMargin = 5;
            params.topMargin = 10;
        }
        params.leftMargin = 20;
        params.rightMargin = 20;
        return params;
    }

    public static boolean hasActionBar(Activity activity) {
        return (((AppCompatActivity) activity).getSupportActionBar() != null);
    }

    public static void launchEmbeddedFragment(String className, Context context) {
        Intent intent = new Intent(context, FragmentContainerActivity.class);
        intent.putExtra(Constants.FRAGMENT_KEY, className);
        context.startActivity(intent);
    }

    public static void setImage(ImageView imageView, byte[] imageBytes, final Context context) {
        final Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        imageView.setImageBitmap(bmp);
        imageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ImageView imageView = new ImageView(context);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                imageView.setImageBitmap(bmp);
                new AlertDialog.Builder(context).setView(imageView).show();
            }
        });
    }

    public static AlertDialog.Builder getMessageDialogBuilder(String caption, String message,
                                                              Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.message_dialog, null);
        ((TextView) view.findViewById(R.id.caption_text)).setText(caption);
        TextView messageTextView = (TextView) view.findViewById(R.id.message);
        messageTextView.setText(Html.fromHtml(message));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        return new AlertDialog.Builder(context).setView(view).setCancelable(false);
    }

    public static void showMessageDialog(AlertDialog.Builder builder) {
        final Dialog dialog = builder.show();
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    return true;
                } else return false;
            }
        });
    }

    //Mandatory dialog, only process if user clicks on dialog buttons
    public static Dialog showMessageDialog(String caption, String message,
                   DialogButton positiveButton, DialogButton negativeButton, Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.message_dialog, null);
        ((TextView) view.findViewById(R.id.caption_text)).setText(caption);
        TextView messageTextView = (TextView) view.findViewById(R.id.message);
        messageTextView.setText(Html.fromHtml(message));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(view)
                .setCancelable(false);
        if (positiveButton != null) builder.setPositiveButton(positiveButton.getLabel(),
                positiveButton.getClickListener());
        if (negativeButton != null) builder.setNegativeButton(negativeButton.getLabel(),
                negativeButton.getClickListener());
        final Dialog dialog = builder.show();
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        /*dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    return true;
                } else return false;
            }
        });*/
        return dialog;
    }

    public static void showPasswordRequiredDialog(final Activity activity) {
        DialogButton positiveButton = new DialogButton(activity.getString(R.string.ok_lbl),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        activity.setResult(Activity.RESULT_CANCELED, new Intent());
                        activity.finish();
                    }
                });
        UIUtils.showMessageDialog(activity.getString(R.string.error_lbl),
                activity.getString(R.string.passw_missing_msg), positiveButton, null, activity);
    }

    public static Toolbar setSupportActionBar(AppCompatActivity activity) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_vs);
        activity.setSupportActionBar(toolbar);
        return toolbar;
    }

    public static Toolbar setSupportActionBar(AppCompatActivity activity, String title) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_vs);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setTitle(title);
        return toolbar;
    }

    public static void killApp(boolean killSafely) {
        if (killSafely) {
            /*
             * Notify the system to finalize and collect all objects of the app
             * on exit so that the virtual machine running the app can be killed
             * by the system without causing issues. NOTE: If this is set to
             * true then the virtual machine will not be killed until all of its
             * threads have closed.
             */
            System.runFinalizersOnExit(true);

            /*
             * Force the system to close the app down completely instead of
             * retaining it in the background. The virtual machine that runs the
             * app will be killed. The app will be completely created as a new
             * app in a new virtual machine running in a new process if the user
             * starts the app again.
             */
            System.exit(0);
        } else {
            /*
             * Alternatively the process that runs the virtual machine could be
             * abruptly killed. This is the quickest way to remove the app from
             * the device but it could cause problems since resources will not
             * be finalized first. For example, all threads running under the
             * process will be abruptly killed when the process is abruptly
             * killed. If one of those threads was making multiple related
             * changes to the database, then it may have committed some of those
             * changes but not all of those changes when it was abruptly killed.
             */
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    public static void fillAddressInfo(LinearLayout linearLayout) throws IOException {
        UserDto user = PrefUtils.getAppUser();
        ((TextView)linearLayout.findViewById(R.id.name)).setText(user.getAddress().getAddress());
        ((TextView)linearLayout.findViewById(R.id.postal_code)).setText(user.getAddress().getPostalCode());
        ((TextView)linearLayout.findViewById(R.id.city)).setText(user.getAddress().getCity());
        linearLayout.setVisibility(View.VISIBLE);
    }

    public static void showConnectionRequiredDialog(final ActivityBase activityBase) {
        if (!App.getInstance().isSocketConnectionEnabled()) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    activityBase.getString(R.string.connection_required_caption),
                    activityBase.getString(R.string.connection_required_msg),
                    activityBase).setPositiveButton(activityBase.getString(R.string.connect_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            LOGD(TAG, "====== TODO showConnectionRequiredDialog");
                            //if(activityBase != null) ConnectionUtils.initConnection(activityBase);
                            dialog.cancel();
                        }
                    });
            UIUtils.showMessageDialog(builder);
        }
    }

}