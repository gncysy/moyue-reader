!macro customInstall
  DetailPrint "安装后端服务..."
  
  # 复制后端文件
  SetOutPath "$INSTDIR\backend"
  File /r "..\backend\build\libs\moyue-backend.jar"
  File /r "..\backend\scripts\install-service.ps1"
  File /r "..\backend\scripts\uninstall-service.ps1"
  
  # 安装服务
  nsExec::ExecToLog 'powershell -ExecutionPolicy Bypass -File "$INSTDIR\backend\install-service.ps1" -JarPath "$INSTDIR\backend\moyue-backend.jar"'
  
  DetailPrint "后端服务安装完成"
!macroend

!macro customUnInstall
  DetailPrint "卸载后端服务..."
  nsExec::ExecToLog 'powershell -ExecutionPolicy Bypass -File "$INSTDIR\backend\uninstall-service.ps1"'
  DetailPrint "后端服务卸载完成"
!macroend
