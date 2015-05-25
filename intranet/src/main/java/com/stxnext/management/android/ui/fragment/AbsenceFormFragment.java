package com.stxnext.management.android.ui.fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Days;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;

import com.actionbarsherlock.ActionBarSherlock;
import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog.OnDateChangedListener;
import com.doomonafireball.betterpickers.calendardatepicker.SimpleMonthAdapter.CalendarDay;
import com.stxnext.management.android.R;
import com.stxnext.management.android.dto.local.MandatedTime;
import com.stxnext.management.android.dto.postmessage.AbsenceMessage;
import com.stxnext.management.android.dto.postmessage.AbsenceMessage.AbsenceType;
import com.stxnext.management.android.dto.postmessage.AbsencePayload;
import com.stxnext.management.android.storage.prefs.StoragePrefs;
import com.stxnext.management.android.ui.dependencies.AsyncTaskEx;
import com.stxnext.management.android.ui.dependencies.Popup;
import com.stxnext.management.android.ui.dependencies.Popup.OnPopupItemClickListener;
import com.stxnext.management.android.ui.dependencies.PopupItem;
import com.stxnext.management.android.ui.dependencies.TimeUtil;
import com.stxnext.management.android.ui.dependencies.TouchResistantEditText;
import com.stxnext.management.android.web.api.HTTPResponse;
import com.stxnext.management.android.web.api.IntranetApi;

public class AbsenceFormFragment  extends Fragment {

    private static final String START_PICKER_TAG = "startPicker";
    private static final String END_PICKER_TAG = "endPicker";
    
    private TableRow startDateRow;
    private TableRow endDateRow;
    private TableRow typeRow;
    
    private EditText absenceExplanationView;
    private TouchResistantEditText absenceTypeView;
    private TouchResistantEditText absenceEndDateView;
    private TextView absenceStartDateView;
    private TouchResistantEditText daysLeftView;
    private Button submitButton;
    
    private View view;
    private Popup typePopup;
    
    Calendar startDate = Calendar.getInstance();
    Calendar endDate = Calendar.getInstance();
    
    CalendarDatePickerDialog startDatePickerDialog;
    CalendarDatePickerDialog endDatePickerDialog;
    
    StoragePrefs prefs;
    IntranetApi api;
    ActionBarSherlock sherlock;
    Integer daysLeft;
    FormActionReceiver formReceiver;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
        prefs = StoragePrefs.getInstance(getActivity());
        api = IntranetApi.getInstance(getActivity().getApplication());
        formReceiver = (FormActionReceiver) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_absence, container,
                false);
        
        startDateRow = (TableRow) view.findViewById(R.id.startDateRow);
        endDateRow = (TableRow) view.findViewById(R.id.endDateRow);
        typeRow = (TableRow) view.findViewById(R.id.typeRow);
        absenceExplanationView = (EditText) view.findViewById(R.id.absenceExplanationView);
        absenceTypeView = (TouchResistantEditText) view.findViewById(R.id.absenceTypeView);
        absenceEndDateView = (TouchResistantEditText) view.findViewById(R.id.absenceEndDateView);
        absenceStartDateView = (TextView) view.findViewById(R.id.absenceStartDateView);
        daysLeftView = (TouchResistantEditText) view.findViewById(R.id.daysLeftView);
        submitButton = (Button) view.findViewById(R.id.submitButton);
        
        startDatePickerDialog = CalendarDatePickerDialog
                .newInstance(null, startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH),
                        startDate.get(Calendar.DAY_OF_MONTH));
        endDatePickerDialog = CalendarDatePickerDialog
                .newInstance(null, endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH),
                        endDate.get(Calendar.DAY_OF_MONTH));
        startDatePickerDialog.registerOnDateChangedListener(new OnDateChangedListener() {
            @Override
            public void onDateChanged() {
               CalendarDay day = startDatePickerDialog.getSelectedDay();
               absenceStartDateView.setText(TimeUtil.updateCalendarAndGetFormat(startDate, day.getYear(), day.getMonth(), day.getDay()));
               startDatePickerDialog.dismiss();
            }
        });
        endDatePickerDialog.registerOnDateChangedListener(new OnDateChangedListener() {
            @Override
            public void onDateChanged() {
                CalendarDay day = endDatePickerDialog.getSelectedDay();
                absenceEndDateView.setText(TimeUtil.updateCalendarAndGetFormat(endDate, day.getYear(), day.getMonth(), day.getDay()));
                endDatePickerDialog.dismiss();
            }
        });
        
        absenceStartDateView.setText(TimeUtil.defaultDateFormat.format(startDate.getTime()));
        absenceEndDateView.setText(TimeUtil.defaultDateFormat.format(endDate.getTime()));
        daysLeft = prefs.getDaysOffToTake();
        if(daysLeft != null){
            daysLeftView.setText(String.valueOf(daysLeft));
        }
        
        configureTypeSelector();
        setActions();
        new UpdateTimeLeft().execute();
        return view;
    }
    
    private void configureTypeSelector(){
        List<PopupItem> items = new ArrayList<PopupItem>();
        for(AbsenceType type : AbsenceType.values()){
            items.add(new PopupItem(getString(type.getResourceId()), type));
        }
        typePopup = new Popup(getActivity(), absenceTypeView);
        typePopup.addItems(items);
        typePopup.setSelected(AbsenceType.PLANNED);
        
        typePopup.setOnItemClickListener(new OnPopupItemClickListener() {
            @Override
            public void onItemClick(Object content) {
                typePopup.setSelected(content);
            }
        });
        typeRow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                typePopup.toggle();
            }
        });
    }
    
    private void setActions(){
        startDateRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                startDatePickerDialog.show(fm,START_PICKER_TAG);
            }
        });
        
        endDateRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                endDatePickerDialog.show(fm,END_PICKER_TAG);
            }
        });
        
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateForm()){
                    AbsenceMessage message = new AbsenceMessage();
                    message.setAbsenceType((AbsenceType) typePopup.getSelected());
                    message.setEndDate(endDate.getTime());
                    message.setStartDate(startDate.getTime());
                    message.setRemarks(absenceExplanationView.getText().toString().trim());
                    formReceiver.onSubmitFormWithMessage(new AbsencePayload(message));
                }
            }
        });
    }

    private class UpdateTimeLeft extends AsyncTaskEx<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            setLoading(true, true);
        };
        
        @Override
        protected Void doInBackground(Void... params) {
            HTTPResponse<MandatedTime> response = api.getDaysOffToTake();
            if(response.ok() && response.getExpectedResponse() != null){
                daysLeft = response.getExpectedResponse().getLeft().intValue();
                prefs.setDaysOffToTake(daysLeft);
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if(getActivity()!=null && !getActivity().isFinishing()){
                daysLeftView.setText(String.valueOf(daysLeft));
                setLoading(false, true);
            }
        }
    }
    
    
//    @Override
//    public void onDateSet(CalendarDatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
//        if(dialog.getTag().equals(START_PICKER_TAG)){
//            absenceStartDateView.setText(TimeUtil.updateCalendarAndGetFormat(startDate, year, monthOfYear, dayOfMonth));
//        }
//        else if(dialog.getTag().equals(END_PICKER_TAG)){
//            absenceEndDateView.setText(TimeUtil.updateCalendarAndGetFormat(endDate, year, monthOfYear, dayOfMonth));
//        }
//        //updateDaysLeft();
//    }
    
    private boolean updateDaysLeft(){
        Days daysBetween = Days.daysBetween(new DateTime(startDate.getTime()), new DateTime(endDate.getTime()));
        int days = daysBetween.getDays();
        int left = (daysLeft - days);
        daysLeftView.setText(String.valueOf(left));
        if(left < 0){
            daysLeftView.setError(getString(R.string.validation_no_days_left));
            daysLeftView.requestFocus();
            return false;
        }
        else if(startDate.after(endDate)){
            absenceEndDateView.setError(getString(R.string.validation_start_date_after_end_date));
            absenceEndDateView.requestFocus();
            return false;
        }
        daysLeftView.setError(null);
        absenceEndDateView.setError(null);
        return true;
    }
    
    private boolean validateForm(){
        return updateDaysLeft();
    }
    
    private void setLoading(boolean loading, boolean affectForm){
        if(sherlock != null){
            sherlock.setProgressBarIndeterminateVisibility(loading);
        }
        if(affectForm){
            setFormEnabled(!loading);
        }
    }
    
    public void setFormEnabled(boolean enabled){
        startDateRow.setEnabled(enabled);
        endDateRow.setEnabled(enabled);
        typeRow.setEnabled(enabled);
        absenceExplanationView.setEnabled(enabled);
        submitButton.setEnabled(enabled);
    }

    public void setSherlock(ActionBarSherlock sherlock) {
        this.sherlock = sherlock;
    }
    
}