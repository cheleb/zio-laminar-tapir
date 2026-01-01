export VERSION=`git describe --tags --abbrev=0 | sed "s/v//"`
echo "Documentation version: $VERSION"
pushd examples/client
npm ci
npm run build
popd
sbt website
