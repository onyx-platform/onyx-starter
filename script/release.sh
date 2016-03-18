#!/bin/bash

set -o errexit
set -o pipefail
set -o nounset
set -o xtrace

cd "$(dirname "$0")/.."

if [[ "$#" -ne 2 ]]; then
	echo "Usage: $0 core-version release-branch"
	echo "Example: $0 0.8.4 0.8.x"
fi

new_core_version=$1
release_branch=$2
current_version=`lein pprint :version | sed s/\"//g`

# Update to release version.
git checkout master
git stash
git pull origin master

lein set-version $new_core_version
lein update-dependency org.onyxplatform/onyx $new_core_version

sed -i.bak "s/$current_version/$new_core_version/g" README.md
git add README.md project.clj

git commit -m "Upgrade to $new_core_version."
git push origin master

# Merge artifacts into release branch.
git checkout -b $release_branch || git checkout $release_branch
git pull origin $release_branch || true
git merge -m "Merge branch 'master' into $release_branch" master -X theirs
git push -u origin $release_branch
