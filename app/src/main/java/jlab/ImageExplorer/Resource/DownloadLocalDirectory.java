package jlab.ImageExplorer.Resource;

import android.os.Handler;
import jlab.ImageExplorer.R;
import java.util.ArrayList;
import java.util.Comparator;
import android.os.Environment;
import jlab.ImageExplorer.Utils;
import static java.util.Collections.sort;

/*
 * Created by Javier on 14/10/2017.
 */
public class DownloadLocalDirectory  extends FilesLocalDirectory {

    public DownloadLocalDirectory(String name) {
        super(name, R.color.orange);
    }

    @Override
    public void openSynchronic(Handler handler) {
        //TODO: Agregar también lo de synchronized(...) en jlab.FileTransfer
        synchronized (monitor) {
            try {
                if (!loaded) {
                    clear();
                    count = 0;
                    ocupedSpace = 0;
                    ArrayList<String> stgPath = Utils.getStoragesPath();
                    Directory allImages = Utils.specialDirectories.getImagesDirectory();
                    allImages.openSynchronic(handler);
                    for (int i = 0; i < stgPath.size(); i++) {
                        String current = stgPath.get(i);
                        if (Utils.existAndMountDir(current)) {
                            loadContentForDir(allImages, String.format("%s/%s/", current, Environment.DIRECTORY_DOWNLOADS));
                            loadContentForDir(allImages, String.format("%s/Android/data/%s/files/", current, Utils.getPackageName()));
                        }
                    }
                    sort(getContent(), new Comparator<Resource>() {
                        @Override
                        public int compare(Resource res1, Resource res2) {
                            if (res1.modification > res2.modification)
                                return -1;
                            if (res1.modification < res2.modification)
                                return 1;
                            return 0;
                        }
                    });
                }
                Utils.lostConnection = false;
            } catch (Exception ignored) {
                Utils.lostConnection = true;
                ignored.printStackTrace();
            } finally {
                loaded = true;
                if (handler != null)
                    handler.sendEmptyMessage(Utils.LOADING_INVISIBLE);
            }
        }
    }

    private void loadContentForDir(Directory directory, String path) {
        for (int i = 0; i < directory.getCountElements(); i++) {
            Resource current = directory.getResource(i);
            if ((Utils.showHiddenFiles || !current.isHidden())
                    && !current.isDir() && ((FileResource) current).isImage()
                    && (path + current.getName()).equals(current.getRelUrl())) {
                LocalFile localFile = (LocalFile) directory.getResource(i);
                addResource(localFile);
            }
        }
    }

    @Override
    public boolean isMultiColumn() {
        return true;
    }

    @Override
    public void loadData() {
        openSynchronic(null);
    }
}