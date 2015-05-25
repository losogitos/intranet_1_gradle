
package com.stxnext.management.android.storage.sqlite.dao;

public class DAO {

    private static DAO _instance;

    public static DAO getInstance() {
        if (_instance == null) {
            _instance = new DAO();
        }
        return _instance;
    }

    IntranetUserDao intranetUser;
    LatenessDao late;
    AbsenceDao absence;

    public void clearAll(){
        intranetUser.clear();
        late.clear();
        absence.clear();
    }
    
    private DAO() {
        intranetUser = new IntranetUserDao();
        late = new LatenessDao();
        absence = new AbsenceDao();
    }

    public IntranetUserDao getIntranetUser() {
        return intranetUser;
    }

    public LatenessDao getLate() {
        return late;
    }

    public AbsenceDao getAbsence() {
        return absence;
    }

}
