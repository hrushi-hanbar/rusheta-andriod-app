package com.example.rusheta.view.ui;

import android.Manifest;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rusheta.service.model.ContactDTO;
import com.example.rusheta.service.model.Contacts;
import com.example.rusheta.service.model.ContactDataDTO;
import com.example.rusheta.service.remote.JsonApiPlaceHolder;
import com.example.rusheta.R;
import com.example.rusheta.utils.CryptoClass;
import com.example.rusheta.view.adapter.MyContactsAdapter;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ContactsActivity extends AppCompatActivity {
    private static final String TAG = "ContactsActivity";
    private static final String BASE_URL = "http://10.0.2.2:3000";
//    private static final String BASE_URL = "http://localhost:3000";
//    private static final String BASE_URL = "https://rusheta.herokuapp.com/";

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    JsonApiPlaceHolder jsonApiPlaceHolder = retrofit.create(JsonApiPlaceHolder.class);

    RecyclerView recyclerView;
    MyContactsAdapter myContactsAdapter;
    ArrayList<ContactDTO> contacts;

    private static final String TAG_ANDROID_CONTACTS = "ANDROID_CONTACTS";


    private class GetContactsTask extends AsyncTask<String, Void, ArrayList<ContactDTO> > {

        protected void onPostExecute(ArrayList<ContactDTO> contacts) {

            SharedPreferences sharedPreferences
                    = getSharedPreferences("RushetaData",
                    MODE_PRIVATE);
            if(sharedPreferences.getString("secret1","").isEmpty()){
                // LOGOUT
                Log.i("GETContactsTASK","Illegal state exception");
                return;
            }

            String key = sharedPreferences.getString("secret1","");
            byte [] keyByte = Base64.decode(key, Base64.DEFAULT);

            String IV = sharedPreferences.getString("secret2","");
            byte [] IVByte = Base64.decode(IV, Base64.DEFAULT);

            try {
                CryptoClass cryptoClass = new CryptoClass(keyByte,IVByte);

                ArrayList<String> contactList = new ArrayList<>();
                for (ContactDTO contact : contacts) {
                    List<ContactDataDTO> i = contact.getPhoneList();
                    if (i.size() != 0) {
                        String ctc = i.get(0).getDataValue();
                        byte[] encCtc = cryptoClass.encrypt(ctc.getBytes());
                        String encCtsString = new String(Base64.encode(encCtc, Base64.DEFAULT));
                        contactList.add(encCtsString);
                    }
                }
                Contacts contactJSON = new Contacts(contactList);

                String token = sharedPreferences.getString("token","");

                Call<Contacts> call = jsonApiPlaceHolder.getContacts(token,contactJSON);

                call.enqueue(new Callback<Contacts>() {
                    @Override
                    public void onResponse(Call<Contacts> call, Response<Contacts> response) {

                        if(!response.isSuccessful()){
                            Log.i("GetContactsNOSucesss",""+response.code());
                            return;
                        }

                        try {

                            List<String> validContacts = response.body().getContacts();
                            Log.i("Valid Contacts",validContacts.toString());
                            Iterator<ContactDTO> iter = contacts.iterator();
                            while (iter.hasNext()) {
                                List<ContactDataDTO> i = iter.next().getPhoneList();
                                if(i.size() != 0) {
                                    if (!validContacts.contains(i.get(0).getDataValue())){
                                        iter.remove();
                                    }
                                }
                            }

                            Log.i("CONTACTS SIZE::", String.valueOf(contacts.size()));
                            myContactsAdapter = new MyContactsAdapter(ContactsActivity.this,contacts);
                            recyclerView.setAdapter(myContactsAdapter);

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<Contacts> call, Throwable t) {
                        t.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        protected ArrayList<ContactDTO> doInBackground(String... strings) {
            return getAllContacts();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        contacts = new ArrayList<>();
        recyclerView = findViewById(R.id.contactsRecyclerView);
        LinearLayoutManager myLinearLayoutManager = new LinearLayoutManager(ContactsActivity.this);
        recyclerView.setLayoutManager(myLinearLayoutManager);
        myContactsAdapter = new MyContactsAdapter(ContactsActivity.this, contacts);
        recyclerView.setAdapter(myContactsAdapter);




        if(!hasPhoneContactsPermission(Manifest.permission.READ_CONTACTS))
            requestPermission(Manifest.permission.READ_CONTACTS);
        else {
            new GetContactsTask().execute();
            Toast.makeText(ContactsActivity.this, "Contact data has been printed in the android monitor log..", Toast.LENGTH_SHORT).show();
        }
    }

    /* Return all contacts and show each contact data in android monitor console as debug info. */
    private ArrayList<ContactDTO> getAllContacts()
    {
        ArrayList<ContactDTO> ret = new ArrayList<>();

        // Get all raw contacts id list.
        List<Integer> rawContactsIdList = getRawContactsIdList();

        int contactListSize = rawContactsIdList.size();

        ContentResolver contentResolver = getContentResolver();

        // Loop in the raw contacts list.
        for(int i=0;i<contactListSize;i++)
        {
            //ContactDTO object
            ContactDTO contact = new ContactDTO();

            // Get the raw contact id.
            Integer rawContactId = rawContactsIdList.get(i);


            //Log.d(TAG_ANDROID_CONTACTS, "raw contact id : " + rawContactId.intValue());

            // Data content uri (access data table. )
            Uri dataContentUri = ContactsContract.Data.CONTENT_URI;

            // Build query columns name array.
            List<String> queryColumnList = new ArrayList<>();

            // ContactsContract.Data.CONTACT_ID = "contact_id";
            queryColumnList.add(ContactsContract.Data.CONTACT_ID);

            // ContactsContract.Data.MIMETYPE = "mimetype";
            queryColumnList.add(ContactsContract.Data.MIMETYPE);

            queryColumnList.add(ContactsContract.Data.DATA1);
            queryColumnList.add(ContactsContract.Data.DATA2);
            queryColumnList.add(ContactsContract.Data.DATA3);
            queryColumnList.add(ContactsContract.Data.DATA4);
            queryColumnList.add(ContactsContract.Data.DATA5);
            queryColumnList.add(ContactsContract.Data.DATA6);
            queryColumnList.add(ContactsContract.Data.DATA7);
            queryColumnList.add(ContactsContract.Data.DATA8);
            queryColumnList.add(ContactsContract.Data.DATA9);
            queryColumnList.add(ContactsContract.Data.DATA10);
            queryColumnList.add(ContactsContract.Data.DATA11);
            queryColumnList.add(ContactsContract.Data.DATA12);
            queryColumnList.add(ContactsContract.Data.DATA13);
            queryColumnList.add(ContactsContract.Data.DATA14);
            queryColumnList.add(ContactsContract.Data.DATA15);

            // Translate column name list to array.
            String queryColumnArr[] = queryColumnList.toArray(new String[queryColumnList.size()]);

            // Build query condition string. Query rows by contact id.
            StringBuffer whereClauseBuf = new StringBuffer();
            whereClauseBuf.append(ContactsContract.Data.RAW_CONTACT_ID);
            whereClauseBuf.append("=");
            whereClauseBuf.append(rawContactId);

            // Query data table and return related contact data.
            Cursor cursor = contentResolver.query(dataContentUri, queryColumnArr, whereClauseBuf.toString(), null, null);

            /* If this cursor return database table row data.
               If do not check cursor.getCount() then it will throw error
               android.database.CursorIndexOutOfBoundsException: Index 0 requested, with a size of 0.
               */
            if(cursor!=null && cursor.getCount() > 0)
            {
                StringBuilder lineBuf = new StringBuilder();
                cursor.moveToFirst();


                lineBuf.append("Raw Contact Id : ");
                lineBuf.append(rawContactId);
                contact.setRawContactId(rawContactId);

                long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                lineBuf.append(" , Contact Id : ");
                lineBuf.append(contactId);
                contact.setContactId(contactId);

                do{
                    // First get mimetype column value.
                    String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
                    lineBuf.append(" \r\n , MimeType : ");
                    lineBuf.append(mimeType);

                    List<String> dataValueList = getColumnValueByMimetype(cursor, mimeType,contact);
                    int dataValueListSize = dataValueList.size();
                    for(int j=0;j < dataValueListSize;j++)
                    {
                        String dataValue = dataValueList.get(j);
                        lineBuf.append(" , ");
                        lineBuf.append(dataValue);
                    }

                }while(cursor.moveToNext());

                Log.d(TAG_ANDROID_CONTACTS, lineBuf.toString());
            }

            cursor.close();
            ret.add(contact);
            Log.d(TAG_ANDROID_CONTACTS, "=========================================================================");
        }

        return ret;
    }

    /*
     *  Get email type related string format value.
     * */
    private String getEmailTypeString(int dataType)
    {
        String ret = "";

        if(ContactsContract.CommonDataKinds.Email.TYPE_HOME == dataType)
        {
            ret = "Home";
        }else if(ContactsContract.CommonDataKinds.Email.TYPE_WORK==dataType)
        {
            ret = "Work";
        }
        return ret;
    }

    /*
     *  Get phone type related string format value.
     * */
    private String getPhoneTypeString(int dataType)
    {
        String ret = "";

        if(ContactsContract.CommonDataKinds.Phone.TYPE_HOME == dataType)
        {
            ret = "Home";
        }else if(ContactsContract.CommonDataKinds.Phone.TYPE_WORK==dataType)
        {
            ret = "Work";
        }else if(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE==dataType)
        {
            ret = "Mobile";
        }
        return ret;
    }

    private List<String> getColumnValueByMimetype(Cursor cursor, String mimeType, ContactDTO contact )
    {
        List<String> ret = new ArrayList<String>();

        switch (mimeType)
        {
            // Get email data.
            case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE :
                // Email.ADDRESS == data1
                String emailAddress = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                // Email.TYPE == data2
                int emailType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                String emailTypeStr = getEmailTypeString(emailType);

                ContactDataDTO obj = new ContactDataDTO(emailType,emailAddress);
                contact.getEmailList().add(obj);

                ret.add("Email Address : " + emailAddress);
                ret.add("Email Int Type : " + emailType);
                ret.add("Email String Type : " + emailTypeStr);
                break;

            // Get nickname
            case ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE:
                // Nickname.NAME == data1
                String nickName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME));
                ret.add("Nick name : " + nickName);
                contact.setNickName(nickName);
                break;

            // Get organization data.
            case ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE:
                // Organization.COMPANY == data1
                String company = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY));
                // Organization.DEPARTMENT == data5
                String department = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT));
                // Organization.TITLE == data4
                String title = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
                // Organization.JOB_DESCRIPTION == data6
                String jobDescription = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION));
                // Organization.OFFICE_LOCATION == data9
                String officeLocation = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION));


                contact.setCompany(company);
                contact.setDepartment(department);
                contact.setTitle(title);
                contact.setJobDescription(jobDescription);
                contact.setOfficeLocation(officeLocation);
                ret.add("Company : " + company);
                ret.add("department : " + department);
                ret.add("Title : " + title);
                ret.add("Job Description : " + jobDescription);
                ret.add("Office Location : " + officeLocation);
                break;

            // Get phone number.
            case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                // Phone.NUMBER == data1
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                // Phone.TYPE == data2
                int phoneTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                String phoneTypeStr = getPhoneTypeString(phoneTypeInt);

                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                try {
                    Phonenumber.PhoneNumber phoneNumberProto = phoneUtil.parse(phoneNumber, "IN");
                    phoneNumber = phoneUtil.format(phoneNumberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
                    Log.i("PHONE NUMBERS",phoneNumber);
                } catch (NumberParseException e) {
                    e.printStackTrace();
                }

                contact.getPhoneList().add(new ContactDataDTO(phoneTypeInt,phoneNumber));
                ret.add("Phone Number : " + phoneNumber);
                ret.add("Phone Type Integer : " + phoneTypeInt);
                ret.add("Phone Type String : " + phoneTypeStr);
                break;

            // Get sip address.
            case ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE:
                // SipAddress.SIP_ADDRESS == data1
                String address = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS));
                // SipAddress.TYPE == data2
                int addressTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.TYPE));
                String addressTypeStr = getEmailTypeString(addressTypeInt);

                contact.getAddressList().add(new ContactDataDTO(addressTypeInt,address));
                ret.add("Address : " + address);
                ret.add("Address Type Integer : " + addressTypeInt);
                ret.add("Address Type String : " + addressTypeStr);
                break;

            // Get display name.
            case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
                // StructuredName.DISPLAY_NAME == data1
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                // StructuredName.GIVEN_NAME == data2
                String givenName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                // StructuredName.FAMILY_NAME == data3
                String familyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));

                if(displayName != null)
                    displayName = displayName.substring(0,1).toUpperCase() + displayName.substring(1).toLowerCase();
                if(givenName != null)
                    givenName = givenName.substring(0,1).toUpperCase() + givenName.substring(1).toLowerCase();
                if(familyName != null)
                    familyName = familyName.substring(0,1).toUpperCase() + familyName.substring(1).toLowerCase();
                contact.setDisplayName(displayName);
                contact.setGivenName(givenName);
                contact.setFamilyName(familyName);
                ret.add("Display Name : " + displayName);
                ret.add("Given Name : " + givenName);
                ret.add("Family Name : " + familyName);
                break;


        }

        return ret;
    }

    // Return all raw_contacts _id in a list.
    private List<Integer> getRawContactsIdList()
    {
        List<Integer> ret = new ArrayList<Integer>();

        ContentResolver contentResolver = getContentResolver();

        // Row contacts content uri( access raw_contacts table. ).
        Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI;
        // Return _id column in contacts raw_contacts table.
        String[] queryColumnArr = {ContactsContract.RawContacts._ID};
        // Query raw_contacts table and return raw_contacts table _id.
        Cursor cursor = contentResolver.query(rawContactUri,queryColumnArr, null, null, null);
        if(cursor!=null)
        {
            cursor.moveToFirst();
            do{
                int idColumnIndex = cursor.getColumnIndex(ContactsContract.RawContacts._ID);
                int rawContactsId = cursor.getInt(idColumnIndex);
                ret.add(rawContactsId);
            }while(cursor.moveToNext());
        }

        cursor.close();

        return ret;
    }

    // Check whether user has phone contacts manipulation permission or not.
    private boolean hasPhoneContactsPermission(String permission)
    {
        boolean ret = false;

        // If android sdk version is bigger than 23 the need to check run time permission.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // return phone read contacts permission grant status.
            int hasPermission = ContextCompat.checkSelfPermission(getApplicationContext(), permission);
            // If permission is granted then return true.
            if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                ret = true;
            }
        }else
        {
            ret = true;
        }
        return ret;
    }

    // Request a runtime permission to app user.
    private void requestPermission(String permission)
    {
        String[] requestPermissionArray = {permission};
        ActivityCompat.requestPermissions(this, requestPermissionArray, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        int length = grantResults.length;
        if(length > 0)
        {
            int grantResult = grantResults[0];

            if(grantResult == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(getApplicationContext(), "You allowed permission, please click the button again.", Toast.LENGTH_LONG).show();
            }else
            {
                Toast.makeText(getApplicationContext(), "You denied permission.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
