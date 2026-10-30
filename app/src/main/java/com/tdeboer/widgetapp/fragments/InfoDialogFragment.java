package com.tdeboer.widgetapp.fragments;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tdeboer.widgetapp.R;

public class InfoDialogFragment extends DialogFragment {

//    android recreates this dialog without our arguments, so the text has to live in the fragment arguments
    public static InfoDialogFragment newInstance(int infoString) {
        InfoDialogFragment fragment = new InfoDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt("info_string", infoString);
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        return new MaterialAlertDialogBuilder(requireContext())
                .setMessage(requireArguments().getInt("info_string"))
                .setPositiveButton(R.string.done, (dialog, id) -> dialog.cancel())
                .create();
    }
}
