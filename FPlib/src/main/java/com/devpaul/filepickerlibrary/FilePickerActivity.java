/*
 * Copyright 2014 Paul Tsouchlos
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.devpaul.filepickerlibrary;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.devpaul.filepickerlibrary.adapter.FileListAdapter;

import java.io.File;

/**
 * Created by Paul Tsouchlos
 */
public class FilePickerActivity extends ListActivity implements NameFileDialogInterface {

    /**
     * Request code for when you want the file path to a directory.
     */
    public static final int REQUEST_DIRECTORY = 101;

    /**
     * Request code for when you want the file path to a specific file.
     */
    public static final int REQUEST_FILE = 102;

    /**
     * Constant value for adding the REQUEST_CODE int as an extra to the {@code FilePickerActivity}
     * {@code Intent}
     */
    public static final String REQUEST_CODE = "requestCode";

    /**
     * Constant value for adding the SCOPE_TYPE enum as an extra to the {@code FilePickerActivity}
     * {@code Intent}
     */
    public static final String SCOPE_TYPE = "scopeType";

    /**
     * Constant for retrieving the return file path in {@link #onActivityResult(int, int, android.content.Intent)}
     * If the result code is RESULT_OK then the file path will not be null. This should always be
     * checked though.
     *
     * Example:
     *
     * <pre>
     * {@code
     *
     * protected void onActivityResult(int resultCode, int requestCode, Intent data) {
     *
     *   if(resultCode == RESULT_OK && requestCode == FILEPICKER) {
     *       String filePath = data.getStringExtra(FilePickerActivity.FILE_EXTRA_DATA_PATH);
     *
     *       if(filePath != null) {
     *           //do something with the string.
     *       }
     *   }
     * }
     * }
     */
    public static final String FILE_EXTRA_DATA_PATH = "fileExtraPath";

    /**
     * List view for list of files.
     */
    private ListView listView;
    /**
     * Button that allows user to selet the file or directory.
     */
    private Button selectButton;
    /**
     * Allows user to enter a directory tree.
     */
    private Button openButton;
    /**
     * Container that encloses the two buttons above.
     */
    private LinearLayout buttonContainer;
    /**
     * {@code TextView} that titles the view.
     */
    private TextView directoryTitle;
    /**
     * {@code ImageButton} that allows for going up one level in a directory tree.
     */
    private ImageButton navUpButton;
    /**
     * {@code ImageButton} that allows the user to create a new folder at the current directory
     */
    private ImageButton newFolderButton;

    /**
     * {@code Animation} for showing the buttonContainer
     */
    private Animation slideUp;
    /**
     * {@code Animation} for hiding the buttonContainer
     */
    private Animation slideDown;
    /**
     * {@code Animation} for showing the navUpButton
     */
    private Animation rotateIn;
    /**
     * {@code Animation} for hiding the navUpButton
     */
    private Animation rotateOut;

    /**
     * {@code File} current directory
     */
    private File curDirectory;
    /**
     * {@code File} the directory one level up from the current one
     */
    private File lastDirectory;
    /**
     * Array of files
     */
    File[] files;
    /**
     * {@code FileListAdapter} object
     */
    private FileListAdapter adapter;
    /**
     * The currently selected file
     */
    private File currentFile;

    private boolean areButtonsShowing;
    private boolean isUpButtonShowing;

    /**
     * {@link com.devpaul.filepickerlibrary.FileType} enum
     */
    private FileType scopeType;
    /**
     * Request code for this activity
     */
    private int requestCode;

    /**
     * {@code Intent} used to send back the data to the calling activity
     */
    private Intent data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        areButtonsShowing = false;
        isUpButtonShowing = false;

        setContentView(R.layout.file_picker_activity_layout);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        //set up the animations
        setUpAnimations();

        //get the scope type and request code. Defaults are all files and request of a directory
        //path.
        scopeType = (FileType) getIntent().getSerializableExtra(SCOPE_TYPE);
        requestCode = getIntent().getIntExtra(REQUEST_CODE, REQUEST_DIRECTORY);

        listView = (ListView) findViewById(android.R.id.list);

        initializeViews();

        curDirectory = new File(Environment.getExternalStorageDirectory().getPath());
        currentFile = new File(curDirectory.getPath());
        lastDirectory = curDirectory.getParentFile();

        if(curDirectory.isDirectory()) {
            new UpdateFilesTask(this).execute(curDirectory);
        } else {
            try {
                throw new Exception("Initial file must be a directory.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initializes all the views in the layout of the activity.
     */
    private void initializeViews() {
        directoryTitle = (TextView) findViewById(R.id.file_directory_title);

        navUpButton = (ImageButton) findViewById(R.id.file_navigation_up_button);
        navUpButton.setVisibility(View.INVISIBLE);
        navUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lastDirectory != null) {
                    new UpdateFilesTask(FilePickerActivity.this).execute(lastDirectory);
                }
            }
        });
        newFolderButton = (ImageButton) findViewById(R.id.new_file_button);
        newFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NameFileDialog nfd = NameFileDialog.newInstance();
                nfd.show(getFragmentManager(), "NameDialog");
            }
        });


        selectButton = (Button) findViewById(R.id.select_button);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(requestCode == REQUEST_DIRECTORY) {
                    if(currentFile.isDirectory()) {
                        curDirectory = currentFile;
                        data = new Intent();
                        data.putExtra(FILE_EXTRA_DATA_PATH, currentFile.getPath());
                        setResult(RESULT_OK, data);
                        finish();
                    }
                } else {
                    if(currentFile.isDirectory()) {
                        curDirectory = currentFile;
                        if(areButtonsShowing) {
                            hideButtons();
                        }
                        new UpdateFilesTask(FilePickerActivity.this).execute(curDirectory);
                    } else {
                        if(!currentFile.isDirectory()) {
                            data = new Intent();
                            data.putExtra(FILE_EXTRA_DATA_PATH, currentFile.getPath());
                            setResult(RESULT_OK, data);
                            finish();
                        }
                    }
                }
            }
        });

        openButton = (Button) findViewById(R.id.open_button);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentFile.isDirectory()) {
                    curDirectory = currentFile;
                    directoryTitle.setText(curDirectory.getName());
                    if(areButtonsShowing) {
                        hideButtons();
                    }
                    new UpdateFilesTask(FilePickerActivity.this).execute(curDirectory);
                } else if (requestCode == REQUEST_FILE) {

                }
            }
        });

        buttonContainer = (LinearLayout) findViewById(R.id.button_container);
        buttonContainer.setVisibility(View.INVISIBLE);
    }

    /**
     * Initializes the animations used in this activity.
     */
    private void setUpAnimations() {
        slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        rotateIn = AnimationUtils.loadAnimation(this, R.anim.rotate_and_fade_in);
        rotateOut = AnimationUtils.loadAnimation(this, R.anim.rotate_and_fade_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        currentFile = files[position];
        adapter.setSelectedPosition(position);
        if(currentFile.isDirectory()) {
            showButtons();
        }
    }

    /**
     * Method that shows the sliding panel
     */
    private void showButtons() {
        if(!areButtonsShowing) {
            buttonContainer.clearAnimation();
            buttonContainer.startAnimation(slideUp);
            buttonContainer.setVisibility(View.VISIBLE);
            areButtonsShowing = true;
        }
    }

    /**
     * Method that hides the sliding panel
     */
    private void hideButtons() {
        if(areButtonsShowing) {
            buttonContainer.clearAnimation();
            buttonContainer.startAnimation(slideDown);
            buttonContainer.setVisibility(View.INVISIBLE);
            areButtonsShowing = false;
        }
    }

    /**
     * Shows the navigation up button.
     */
    private void showUpButton() {
        if(!isUpButtonShowing) {
            navUpButton.clearAnimation();
            navUpButton.startAnimation(rotateIn);
            navUpButton.setVisibility(View.VISIBLE);
            isUpButtonShowing = true;
        }
    }

    /**
     * Hides the navigation up button.
     */
    private void hideUpButton() {
        if(isUpButtonShowing) {
            navUpButton.clearAnimation();
            navUpButton.startAnimation(rotateOut);
            navUpButton.setVisibility(View.INVISIBLE);
            isUpButtonShowing = false;
        }
    }

    @Override
    public void onReturnFileName(String fileName) {

        if(fileName.equalsIgnoreCase("") || fileName.isEmpty()) {
            fileName = "New Folder";
        }
        File file = new File(curDirectory.getPath() + "//" + fileName);
        if(!file.exists()) {
            file.mkdirs();
        }
        new UpdateFilesTask(this).execute(curDirectory);
    }

    /**
     * Set the background drawable of the header
     * @param background {@code Drawable} to use.
     */
    public void setHeaderBackground(Drawable background) {
        if(background != null) {
            buttonContainer.setBackgroundDrawable(background);
        }
    }

    /**
     * Set the background color of the header
     * @param colorId Resource Id of the color
     */
    public void setHeaderBackground(int colorId) {

        if(colorId != 0) {
            try {
                buttonContainer.setBackgroundColor(getResources().getColor(colorId));
            } catch(Exception e) {
                buttonContainer.setBackgroundColor(getResources()
                        .getColor(android.R.color.holo_blue_light));
                e.printStackTrace();
            }
        }

    }

    /**
     * Class that updates the list view with a new array of files. Resets the adapter and the
     * directory title.
     */
    private class UpdateFilesTask extends AsyncTask<File, Void, File[]> {

        private File[] fileArray;
        private Context mContext;
        private ProgressDialog dialog;
        private File directory;

        private UpdateFilesTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(mContext);
            dialog.setMessage("Loading...");
            dialog.setCancelable(false);
            dialog.show();
            super.onPreExecute();
        }

        @Override
        protected File[] doInBackground(File... files) {
            directory = files[0];
            fileArray = files[0].listFiles();
            return fileArray;
        }

        @Override
        protected void onPostExecute(File[] localFiles) {
            files = localFiles;
            if(directory.getPath().equalsIgnoreCase(Environment
                    .getExternalStorageDirectory().getPath())) {
                hideUpButton();
                directoryTitle.setText("Parent Directory");
            } else {
                directoryTitle.setText(directory.getName());
                showUpButton();
            }
            lastDirectory = directory.getParentFile();

            adapter = new FileListAdapter(FilePickerActivity.this, files, scopeType);
            setListAdapter(adapter);
            if(dialog.isShowing()) {
                dialog.dismiss();
            }
            super.onPostExecute(files);
        }
    }
}