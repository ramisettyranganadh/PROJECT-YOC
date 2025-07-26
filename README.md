README.poky.md

1. sudo apt-get install build-essential chrpath cpio debianutils diffstat file gawk gcc git iputils-ping libacl1 liblz4-tool locales python3 python3-git python3-jinja2 python3-pexpect python3-pip python3-subunit socat texinfo unzip wget xz-utils zstd
2. sudo apparmor_parser -R /etc/apparmor.d/unprivileged_userns;
3. bitbake core-image-base; runqemu qemux86-64;

----------------------Start Yocto with Layers------------------------------

1. mkdir Yocto; cd Yocto;
2. git clone https://github.com/yoctoproject/poky.git; git checkout origin/styhead;
3. bitbake virtual/bootloader
4. bitbake -c menuconfig virtual/kernel

For Raspberry Pi :
1. git clone https://github.com/agherzan/meta-raspberrypi.git;
2. cd poky; source oe-init-build-env; bitbake -c menuconfig virtual/kernel
3. bitbake-layers add-layer ../../meta-raspberrypi;
Add in local.conf:
1. MACHINE = "raspberrypi5"
2. LICENSE_FLAGS_ACCEPTED = "synaptics-killswitch"
3. RPI_USE_U_BOOT = "1"

For Beagle Bone Black :
1. git clone https://github.com/Angstrom-distribution/meta-ti.git;
2. cd poky; source oe-init-build-env; bitbake -c menuconfig virtual/kernel
3. bitbake-layers add-layer ../../meta-ti;
Add in local.conf:
1. MACHINE = "beaglebone-yocto"
2. LICENSE_FLAGS_ACCEPTED = "synaptics-killswitch"

-------------------Build & Flash Image on SD Card--------------------------

1. sudo apparmor_parser -R /etc/apparmor.d/unprivileged_userns; bitbake core-image-base;
2. lsblk; sudo umount /dev/mmcblk0; sudo mkfs.vfat -F 32 /dev/mmcblk0;
3. cd tmp/deploy/images/raspberrypi5/
4. bzip2 -d -f core-image-base-raspberrypi5.rootfs.wic.bz2
5. sudo dd if=core-image-base-raspberrypi5.rootfs.wic of=/dev/mmcblk0 status=progress bs=4M

---------------------------Load Images from UART---------------------------

https://github.com/niekiran/EmbeddedLinuxBBB/tree/master/pre-built-images/serial-boot

1. Press S2 and Power ON, Press Ctrl A + S and select Xmodem then select u-boot-spl.bin, u-boot.img
2. Press space and Enter to select and transfer a file over Serial Xmodem
3. Enter UBOOT -> loadx 0x82000000 -> Press Ctrl A + S then select uImage
4. Enter UBOOT -> loadx 0x88000000 -> Press Ctrl A + S then select dtb
5. Enter UBOOT -> loadx 0x88080000 -> Press Ctrl A + S then select initramfs
6. Enter UBOOT -> setenv bootargs console=ttyO0,115200 root=/dev/ram0 rw initrd=0x88080000
7. Enter UBOOT -> bootm 0x82000000 0x88080000 0x88000000

----------------------Debug Probe Serial Communication---------------------

1. minicom -b 115200 -o -D /dev/ttyACM0
2. Press any Key within 3 Seconds to U-BOOT
3. printenv; env set ranganadh ramisetty; printenv ranganadh;

--------------------------Custom Yocto Layer-------------------------------

1. git clone https://github.com/ramisettyranganadh/PROJECT-YOC.git
2. bitbake-layers create-layer meta-mylayer (OR)
   git clone https://github.com/ramisettyranganadh/PROJECT-MLR.git
3. cd PROJECT-YOC; source oe-init-build-env;
4. bitbake-layers add-layer ../../PROJECT-MLR;

----------------------Add WiFi Recipe in Yocto-----------------------------

1. Add below commands in local.conf file:
	IMAGE_INSTALL:append = " wpa-supplicant"
2. Create wpa-supplicant_%.bbappend (% is same version as wpa-supplicant_%.bb)in /meta/recipes-connectivity/wpa-supplicant/ and add below commands:
	SYSTEMD_AUTO_ENABLE:wpa-supplicant = "enable"
	SYSTEMD_SERVICE:wpa-supplicant = "wpa_supplicant.service"
3. Add below commands in /meta/recipes-connectivity/wpa-supplicant/wpa-supplicant.conf file:
	ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
	update_config=1
	country=<Your_Country_Code>  # e.g., US, IN, etc.
	network={
	    ssid="<YourSSID>"
	    psk="<YourPassword>"
	}
4. Execute below bitbake command:
	bitbake core-image-minimal
5. Add the below commands in local.conf file if systemctl is not found:
	DISTRO_FEATURES:append = " usrmerge"
	DISTRO_FEATURES:append = " systemd"
	VIRTUAL-RUNTIME_init_manager = "systemd"
	VIRTUAL-RUNTIME_initscripts = "systemd-compat-units"
	(OR)
	DISTRO_FEATURES:append = " usrmerge"
	DISTRO_FEATURES:append = " systemd"
	VIRTUAL-RUNTIME_init_manager = "systemd"
	VIRTUAL-RUNTIME_initscripts = ""
	VIRTUAL-RUNTIME_sysvinit = ""
	(OR)
	DISTRO_FEATURES:remove = " systemd"
	VIRTUAL-RUNTIME_init_manager = "sysvinit"
6. sudo apparmor_parser -R /etc/apparmor.d/unprivileged_userns
7. bitbake core-image-minimal -c cleanall
8. bitbake core-image-minimal

Kernel Develop and Debug in Yocto:
$ devtool modify linux-yocto
$ cd build/workspace/sources/linux-yocto/
Add printk in init/calibrate.c calibrate_delay
$ devtool build linux-yocto
$ cd ~
$ devtool build-image core-image-base
$ runqemu qemux86
$ dmesg | less
$ cd poky_sdk/workspace/sources/linux-yocto
$ git status
$ git add init/calibrate.c
$ git commit -m "calibrate: Add printk example"
$ devtool finish linux-yocto ~/PROJECT-MLR
$ cd poky/build
$ bitbake core-image-base

