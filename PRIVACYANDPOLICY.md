# Vectras VM — Privacy Policy

Last updated: October 8, 2026

## 1. Scope and contact

This policy describes the current maintained Android build of Vectras VM available through https://github.com/xoureldeen/Vectras-VM-Android. It covers the app's own behavior, not independent forks, software running inside a guest operating system, external websites, or services you choose to use.

Project-maintainer contact: noureldeenelsayed1212@gmail.com
Download-metadata repository: https://github.com/xoureldeen/vectras-vm-bucket

This build does not require a Vectras account and does not implement a first-party cloud account, virtual-disk synchronization service, Firebase Analytics, Firebase Cloud Messaging, Firebase Crashlytics, advertising SDK, or likes/view-count service. This does not mean the app is entirely offline: the network features described below contact external providers.

## 2. Information processed on your device

The app stores and processes information locally to provide its features, including:

- Virtual-machine names and settings, virtual disks, ROM and CVBI packages, imported drives, selected file paths, shared-folder files, and exports.
- App preferences such as language, theme, architecture, display, networking, and file-picker settings.
- Bootstrap and Linux-environment files, installed QEMU and graphics packages, downloaded archives, and temporary or cached files.
- Terminal input and output, runtime logs, and diagnostic information needed to show local errors and system status.
- Clipboard content when you copy or paste information or enable the available host/guest clipboard integration.

Local crash reports can include the time of the error, device manufacturer and model, Android and app versions, kernel version, supported CPU architectures, Android build fingerprint, and a stack trace. Logs and command output can also contain file paths or other information produced by the software you run. The current crash-reporting code writes a local report; it does not automatically upload that report to the maintainer.

The app does not automatically upload your virtual disks, documents, terminal history, clipboard contents, or local crash reports to a project server. Files and data may nevertheless leave your device through guest software, commands, networking, external services, or sharing actions you initiate.

## 3. Network requests and third-party providers

Depending on the feature you use and your settings, the app can make requests for:

- ROM and software catalogs, update metadata, and setup-file metadata hosted in the project download repository.
- QEMU archives, graphics packages, app-update downloads, store icons, and files or provider pages referenced by those catalogs.
- Public GitHub contributor profiles and avatar images displayed in About.
- The Terms of Service and Privacy Policy from the root of the source repository when you open their dialogs. A bundled copy is available if the online document cannot be loaded.
- Linux packages from repositories configured in the local Linux environment when package installation or updates run.

Opening a catalog or About can trigger requests even if you do not download a ROM. Downloads and image requests contact the destination selected by metadata; that destination is not necessarily operated by this project's maintainer.

As with ordinary internet requests, a destination provider and relevant network infrastructure can receive your IP address, request time, requested resource, and normal protocol or client-header information. Selecting an architecture-specific download can reveal which package architecture you requested. HTTPS is used for the configured project metadata and legal-document URLs, but the app also allows HTTP traffic for compatible services and local integrations; not every user-configured connection is encrypted.

Third-party providers determine how their own request records are used and retained. GitHub's policy is available at https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement. Consult the policies of other download hosts, image providers, package mirrors, websites, and social services you use. Those providers may process information in countries different from your own.

## 4. Information you choose to share

The About contact action opens an email draft through an email application you select. The draft includes the device brand in its subject and the device model in its body. The maintainer receives your email address, message, and any attachments only if you send them. Your email provider processes the message under its own policies.

You can also copy logs, export or share VM packages and files, or publish information through services such as GitHub. Public issues and comments may be visible to anyone. Review and remove secrets, account details, personal file paths, and unrelated information before sharing. Support messages are used to understand and respond to the request you submit; do not assume that deleting local app data deletes an email or public post.

Guest operating systems, guest applications, and terminal commands have their own network and data-handling behavior. Shared folders, clipboard integration, network forwarding, and external-display options can make information available outside a guest. Only enable integrations you intend to use, and review the privacy practices of guest software separately.

## 5. Permissions and device access

The manifest declares storage and media permissions, including broad external-storage access, to support local file management. The app can request storage access and notification permission; Android version, runtime grants, and your settings determine what is actually available.

Other declared capabilities include internet and network-state access, audio settings and microphone permission, foreground services, wake locks, vibration, overlay access, battery-optimization exemptions, package visibility, and secure-settings access. Some require special user or system authorization and may not be available in an ordinary installation. The app checks for available companion or external applications when opening supported features.

A declared permission is not proof that all related information is captured. No background microphone-upload or personal-profile collection service is implemented in the reviewed app code. Programs you run can use resources made available to them, subject to Android's restrictions. You can review and revoke granted permissions in Android settings; doing so may limit some features.

## 6. Retention, deletion, and Android backup

App preferences, environment files, and other local data remain until they are overwritten, deleted, or removed by Android or by your actions. The local last-crash report is replaced when another crash report is written. Cache files may be cleared independently. Deleting a VM entry does not necessarily delete every disk, export, or shared file associated with it; inspect your storage before deleting important files.

You can delete files through the application's available file-management features or a file manager, and clear app-managed data or uninstall through Android settings. Clearing data or uninstalling can destroy app-managed files. Export needed files first. Shared-storage files, exported packages, copies held by recipients, and remote backups may remain and must be managed separately.

Android backup is enabled in the current manifest. Eligible app data may be backed up or transferred by Android or a device-vendor service according to your device settings and the applicable platform rules. The project does not operate that backup service or control its retention. Review your device's backup and account settings; see https://developer.android.com/identity/data/autobackup for platform information.

There is no in-app account-deletion workflow because this build has no Vectras account system. To ask about a support message you sent, contact the maintainer using the address above. Third-party request logs, public posts, and backup copies are managed by their respective providers, not by uninstalling this app.

## 7. Security and your choices

Local storage and Android permission controls provide some protection, but the app is not a guarantee of secure isolation or confidential storage. The current build does not add a dedicated encryption layer for virtual disks or diagnostic logs. An unlocked, compromised, or broadly shared device can expose local data. Some guest services and local integrations are not encrypted.

Use trusted files, keep independent backups, protect your Android device, avoid exposing unprotected services, and do not put confidential files into shared folders unnecessarily. You can choose a local QEMU archive instead of an online setup download, avoid store and social features, review available settings, disable clipboard synchronization when appropriate, and avoid sending support messages. Guest networking must be configured separately; choosing a local archive alone does not prevent other network requests.

## 8. Privacy requests and younger users

Depending on applicable law and the information involved, you may have rights to request access, correction, deletion, restriction, portability, or to object to processing, and to contact a relevant supervisory authority. Send requests about information you supplied to the maintainer to the contact address above. The maintainer cannot access or delete files stored only on your device, or directly control records held by independent providers. Do not send identification documents unless they are necessary and specifically requested for a legitimate purpose.

The app has no age-verification or child-account system. If a younger user contacts the maintainer, they should avoid sharing unnecessary personal information and involve a parent or guardian where appropriate. If you believe a child's personal information was included in a support request or public project post, contact the maintainer to discuss removal; third-party copies may require separate requests.

## 9. Updates to this policy

This policy reflects the current app's implemented features and declared capabilities. Changes to networking, diagnostics, permissions, or project services can require an updated notice. Revisions will carry an updated date in the source repository. The app attempts to show the online document when opened and identifies the bundled copy if that request fails. Modified builds and older releases may differ.
