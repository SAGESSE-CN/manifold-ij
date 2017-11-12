package manifold.ij.extensions;

import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportProviderBase;
import com.intellij.ide.util.frameworkSupport.FrameworkVersion;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import manifold.ij.core.ManProject;
import manifold.ij.fs.IjFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 */
public class ManSupportProvider extends FrameworkSupportProviderBase
{
  protected ManSupportProvider()
  {
    super( "manifold", "Manifold" );
  }

  @Nullable
  @Override
  public Icon getIcon()
  {
    return new ImageIcon( getClass().getResource( "/manifold/ij/icons/manifold_20.png" ) );
  }

  @Override
  protected void addSupport( @NotNull Module module, @NotNull ModifiableRootModel rootModel, FrameworkVersion version, @Nullable Library library )
  {
    addToolsJar( rootModel );
  }

  private void addToolsJar( @NotNull ModifiableRootModel rootModel )
  {
    if( hasToolsJar( rootModel ) )
    {
      return;
    }

    VirtualFile toolsJarFile = findToolsJarFile( rootModel );
    if( toolsJarFile == null )
    {
      Notifications.Bus.notify( new Notification( "Project JDK", "tools.jar not found!", "Please add tools.jar to your JDK", NotificationType.ERROR ) );
      return;
    }

    SdkModificator sdkModificator = rootModel.getSdk().getSdkModificator();
    sdkModificator.addRoot( toolsJarFile, OrderRootType.CLASSES );
    sdkModificator.commitChanges();
  }

  private boolean hasToolsJar( ModifiableRootModel rootModel )
  {
    for( VirtualFile file : rootModel.getSdk().getRootProvider().getFiles( OrderRootType.CLASSES ) )
    {
      if( file.getName().equalsIgnoreCase( "tools.jar" ) )
      {
        return true;
      }
    }
    return false;
  }

  private VirtualFile findToolsJarFile( ModifiableRootModel rootModel )
  {
    File file = new File( System.getProperty( "java.home" ) );
    if( file.getName().equalsIgnoreCase( "jre" ) )
    {
      file = file.getParentFile();
    }
    String[] defaultToolsLocation = {"lib", "tools.jar"};
    for( String name : defaultToolsLocation )
    {
      file = new File( file, name );
    }

    if( !file.exists() )
    {
      return null;
    }

    IjFile ijFile = (IjFile)ManProject.manProjectFrom( rootModel.getProject() ).getFileSystem().getIFile( file );
    return ijFile.getVirtualFile();
  }

  @NotNull
  @Override
  public FrameworkSupportConfigurable createConfigurable( @NotNull FrameworkSupportModel model )
  {
    return new ManFrameworkSupportConfigurable( this, model );
  }

  @Override
  public boolean isEnabledForModuleType( @NotNull ModuleType moduleType )
  {
    return true;
  }
}
