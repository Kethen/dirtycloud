set -xe

podman run --rm -it \
	--arch i386 \
	-v './:/out' \
	debian:bookworm \
	bash -c '
		set -xe
		apt update; apt install -y nextcloud-desktop-cmd busybox-static ca-certificates zip

		mkdir -p /package/dev
		mkdir -p /package/proc
		mkdir -p /package/sys
		mkdir -p /package/tmp
		mkdir -p /package/run
		cp -a etc usr var /package/
		ln -s usr/lib /package/lib
		cd /package/
		rm -f /out/app/src/main/assets/i386.tar
		tar -cf /out/app/src/main/assets/i386.tar .

		cd /
		rm -r /package

		mkdir -p /package/lib/x86
		mkdir -p /package/lib/x86_64

		if false
		then
		cp /usr/bin/busybox package/lib/x86
		tar -C /tmp/ -xf /out/build-proot-android/packages/proot-android-i686.tar.gz
		cp /tmp/root/bin/proot package/lib/x86
		cp /tmp/root/libexec/proot/loader package/lib/x86
		tar -C /tmp/ -xf /out/build-proot-android/packages/proot-android-x86_64.tar.gz
		cp /tmp/root/bin/proot package/lib/x86_64
		cp /tmp/root/libexec/proot/loader package/lib/x86_64
		cp /tmp/root/libexec/proot/loader32 package/lib/x86_64
		fi

		if true
		then
		cp /out/termux_proot_prebuilt/i686/data/data/com.termux/files/usr/bin/proot package/lib/x86
		cp /out/termux_proot_prebuilt/i686/data/data/com.termux/files/usr/libexec/proot/loader package/lib/x86
		cp /out/termux_proot_prebuilt/i686/data/data/com.termux/files/usr/lib/libtalloc.so.2.4.3 package/lib/x86/libtalloc.so.2
		cp /usr/bin/busybox package/lib/x86_64
		cp /out/termux_proot_prebuilt/x86_64/data/data/com.termux/files/usr/bin/proot package/lib/x86_64
		cp /out/termux_proot_prebuilt/x86_64/data/data/com.termux/files/usr/libexec/proot/loader package/lib/x86_64
		cp /out/termux_proot_prebuilt/x86_64/data/data/com.termux/files/usr/libexec/proot/loader32 package/lib/x86_64
		cp /out/termux_proot_prebuilt/x86_64/data/data/com.termux/files/usr/lib/libtalloc.so.2.4.3 package/lib/x86_64/libtalloc.so.2
		fi

		cd /package
		rm -f /out/app/libs/x86.jar
		zip -r -y -9 /out/app/libs/x86.jar lib
	'

podman run --rm -it \
	--arch armhf \
	-v './:/out' \
	debian:bookworm \
	bash -c '
		set -xe
		apt update; apt install -y nextcloud-desktop-cmd busybox-static ca-certificates zip

		mkdir -p /package/dev
		mkdir -p /package/proc
		mkdir -p /package/sys
		mkdir -p /package/tmp
		mkdir -p /package/run
		cp -a etc usr var /package/
		ln -s usr/lib /package/lib
		cd /package/
		rm -f /out/app/src/main/assets/armhf.tar
		tar -cf /out/app/src/main/assets/armhf.tar .

		cd /
		rm -r /package

		mkdir -p /package/lib/armeabi-v7a
		mkdir -p /package/lib/arm64-v8a

		cp /out/termux_proot_prebuilt/arm/data/data/com.termux/files/usr/bin/proot package/lib/armeabi-v7a
		cp /out/termux_proot_prebuilt/arm/data/data/com.termux/files/usr/libexec/proot/loader package/lib/armeabi-v7a
		cp /out/termux_proot_prebuilt/arm/data/data/com.termux/files/usr/lib/libtalloc.so.2.4.3 package/lib/armeabi-v7a/libtalloc.so.2
		cp /usr/bin/busybox package/lib/armeabi-v7a
		cp /out/termux_proot_prebuilt/aarch64/data/data/com.termux/files/usr/bin/proot package/lib/arm64-v8a
		cp /out/termux_proot_prebuilt/aarch64/data/data/com.termux/files/usr/libexec/proot/loader package/lib/arm64-v8a
		cp /out/termux_proot_prebuilt/aarch64/data/data/com.termux/files/usr/libexec/proot/loader32 package/lib/arm64-v8a
		cp /out/termux_proot_prebuilt/aarch64/data/data/com.termux/files/usr/lib/libtalloc.so.2.4.3 package/lib/arm64-v8a/libtalloc.so.2

		cd /package
		rm -f /out/app/libs/arm.jar
		zip -r -y -9 /out/app/libs/arm.jar lib
	'
