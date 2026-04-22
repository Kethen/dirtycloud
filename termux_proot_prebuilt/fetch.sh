set -xe

script_dir="$(dirname $0)"
script_dir="$(realpath $script_dir)"
base_url="https://packages.termux.dev/apt/apt/termux-main/pool/main/p/proot/"
package_prefix=proot_5.1.107-70_

cd $script_dir
rm *.deb


wget https://packages.termux.dev/apt/apt/termux-main/pool/main/p/proot/proot_5.1.107-70_x86_64.deb
wget https://packages.termux.dev/apt/apt/termux-main/pool/main/p/proot/proot_5.1.107-70_i686.deb
wget https://packages.termux.dev/apt/apt/termux-main/pool/main/p/proot/proot_5.1.107-70_arm.deb
wget https://packages.termux.dev/apt/apt/termux-main/pool/main/p/proot/proot_5.1.107-70_aarch64.deb

for arch in x86_64 i686 arm aarch64
do
	wget "${base_dir}${package_prefix}${arch}.deb"
	cd "$script_dir"
	rm -rf $arch
	mkdir $arch
	cd $arch
	ar xv "../${package_prefix}${arch}.deb"
	tar -xf data.tar.xz
done
