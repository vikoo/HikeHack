package com.teachmate.teachmate;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.teachmate.teachmate.DBHandlers.DeviceInfoDBHandler;
import com.teachmate.teachmate.models.DeviceInfoModel;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.fragment_settings, container, false);

        Switch mySwitch = (Switch) layout.findViewById(R.id.switchDndMode);

        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    DeviceInfoModel info = new DeviceInfoModel();
                    info.Key = "DND";
                    info.Value = "true";
                    DeviceInfoDBHandler.InsertOrUpdate(getActivity().getApplicationContext(), info);
                }
                else{
                    DeviceInfoModel info = new DeviceInfoModel();
                    info.Key = "DND";
                    info.Value = "false";
                    DeviceInfoDBHandler.InsertOrUpdate(getActivity().getApplicationContext(), info);
                }
            }
        });
        return layout;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(FragmentTitles.SETTINGS);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
